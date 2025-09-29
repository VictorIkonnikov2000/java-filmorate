package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.validate.FilmValidate;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component("FilmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate,
                         @Qualifier("GenreDbStorage") GenreStorage genreStorage,
                         @Qualifier("MpaRatingDbStorage") MpaRatingStorage mpaRatingStorage) {
        this.jdbcTemplate = jdbcTemplate;
        this.genreStorage = genreStorage;
        this.mpaRatingStorage = mpaRatingStorage;
    }

    @Override
    public Film createFilm(Film film) {
        if (!FilmValidate.validateFilm(film)) {
            throw new IllegalArgumentException("Film validation failed");
        }

        // Проверяем существование MPA-рейтинга.
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaRatingStorage.getMpaById(film.getMpa().getId());
        }

        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        Long filmId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        film.setId(filmId);

        // Сохраняем жанры с использованием batchUpdate
        saveFilmGenres(film);

        // Возвращаем полностью заполненный объект фильма, включая данные MPA и жанры, загруженные по его новому ID.
        return getFilmById(filmId);
    }

    @Override
    public Film updateFilm(Film film) {
        if (!FilmValidate.validateFilm(film)) {
            throw new IllegalArgumentException("Film validation failed");
        }

        // Проверяем существование фильма
        getFilmById(film.getId());

        // Проверяем существование MPA-рейтинга
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaRatingStorage.getMpaById(film.getMpa().getId());
        }

        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        int rowsAffected = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() == null ? null : film.getMpa().getId(),
                film.getId());

        if (rowsAffected == 0) {
            throw new NotFoundException("Film not found with ID: " + film.getId());
        }

        // Обновляем жанры фильма
        updateFilmGenres(film);

        // Возвращаем полностью заполненный объект фильма после обновления.
        return getFilmById(film.getId());
    }

    // --- Новый метод для пакетной вставки жанров ---
    private void saveFilmGenres(Film film) {
        // Если у фильма нет жанров, ничего не делаем.
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }

        // Удаляем дубликаты и приводим в отсортированный список для консистентности.
        // Это также упростит сравнение и обновление в будущем, если потребуется.
        List<Long> distinctGenreIds = film.getGenres().stream()
                .map(Genre::getId)
                .filter(Objects::nonNull) // Фильтруем null ID
                .distinct() // Убираем дубликаты
                // Проверяем, существует ли каждый жанр
                .peek(genreId -> genreStorage.getGenreById(genreId)
                        .orElseThrow(() -> new NotFoundException("Genre not found with ID: " + genreId)))
                .collect(Collectors.toList());

        if (distinctGenreIds.isEmpty()) {
            return; // Если все жанры были null или дубликатами, или не найдены после фильтрации
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";

        // Используем batchUpdate для пакетной вставки
        jdbcTemplate.batchUpdate(sql, distinctGenreIds, 100, (ps, genreId) -> {
            ps.setLong(1, film.getId());
            ps.setLong(2, genreId);
        });
    }

    // --- Метод для обновления жанров фильма ---
    private void updateFilmGenres(Film film) {
        // Сначала удаляем все связи жанров для данного фильма
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        // Затем вставляем новые жанры (если они есть)
        saveFilmGenres(film);
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name AS mpa_name " +
                "FROM films AS f JOIN mpa_ratings AS mr ON f.mpa_id = mr.mpa_id";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper());
        // Изменено: film.setGenres(getFilmGenres(film.getId()))
        films.forEach(film -> film.setGenres(getFilmGenres(film.getId()))); // Загружаем жанры
        return films;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        getFilmById(filmId);

        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, filmId, userId);
        } catch (DataIntegrityViolationException e) {
            // Если лайк уже существует (unique constraint violation) или пользователь/фильм не найден (FK violation)
            // В зависимости от логики, можно выбросить другое исключение или проигнорировать.
            // Например, если FK настроены корректно, DataIntegrityViolationException будет выброшен,
            // если filmId или userId не существуют.
            throw new IllegalArgumentException("Failed to add like: " + e.getMessage());
        }
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        getFilmById(filmId);

        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, filmId, userId);
        if (rowsAffected == 0) {
            throw new NotFoundException("Like not found for film ID: " + filmId + " and user ID: " + userId);
        }
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name AS mpa_name " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "JOIN mpa_ratings AS mr ON f.mpa_id = mr.mpa_id " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name " +
                "ORDER BY COUNT(l.user_id) DESC " +
                "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper(), count);
        // Изменено: film.setGenres(getFilmGenres(film.getId()))
        films.forEach(film -> film.setGenres(getFilmGenres(film.getId())));
        return films;
    }

    @Override
    public Film getFilmById(Long filmId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name AS mpa_name " +
                "FROM films AS f JOIN mpa_ratings AS mr ON f.mpa_id = mr.mpa_id WHERE f.film_id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper(), filmId);
            // Изменено: film.setGenres(getFilmGenres(filmId))
            film.setGenres(getFilmGenres(filmId));
            return film;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Film not found with ID: " + filmId);
        }
    }

    // --- Метод для загрузки жанров фильма ---
    // Этот метод теперь будет использовать genreStorage для получения объектов Genre
    private List<Genre> getFilmGenres(Long filmId) { // Изменено с Set<Genre> на List<Genre>
        String sql = "SELECT fg.genre_id FROM film_genres AS fg WHERE fg.film_id = ?";
        List<Long> genreIds = jdbcTemplate.queryForList(sql, Long.class, filmId);

        if (genreIds.isEmpty()) {
            return Collections.emptyList(); // Изменено с emptySet() на emptyList()
        }

        // Используем genreStorage для загрузки полных объектов Genre
        Collection<Genre> genres = genreStorage.getGenresByIds(genreIds);
        return genres.stream()
                .sorted((g1, g2) -> Long.compare(g1.getId(), g2.getId())) // Сортируем по ID
                .collect(Collectors.toList()); // Собираем в List
    }

    // Вспомогательный RowMapper для Film
    private RowMapper<Film> filmRowMapper() {
        return (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));
            // Создаем объект MpaRating
            film.setMpa(MpaRating.builder()
                    .id(rs.getLong("mpa_id"))
                    .name(rs.getString("mpa_name"))
                    .build());
            return film;
        };
    }
}




