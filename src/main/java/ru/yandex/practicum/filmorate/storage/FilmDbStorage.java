package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component("FilmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreStorage genreStorage; // Внедряем GenreStorage
    private final MpaRatingStorage mpaRatingStorage; // Внедряем MpaRatingStorage

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
        // Выполняем предварительную валидацию объекта Film.
        if (!FilmValidate.validateFilm(film)) {
            throw new IllegalArgumentException("Film validation failed");
        }

        // Проверяем существование MPA-рейтинга. Если MPA указан, но не найден в базе, выбрасываем исключение.
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            // Получаем MPA-рейтинг по ID. Если он не найден, будет выброшено исключение NotFoundException
            // внутри метода getMpaById, который затем обрабатывается слоем сервиса или контроллера.
            // Здесь мы просто вызываем метод, чтобы убедиться в его существовании.
            mpaRatingStorage.getMpaById(film.getMpa().getId());
        }

        // SQL-запрос для вставки нового фильма в таблицу 'films'.
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        // Обновляем базу данных, используя PreparedStatement для безопасной вставки данных
        // и KeyHolder для получения сгенерированного базой ID фильма.
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            ps.setLong(5, film.getMpa().getId());
            return ps;
        }, keyHolder);

        // Получаем сгенерированный ID фильма и устанавливаем его в объект Film.
        Long filmId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        film.setId(filmId);

        // Сохраняем жанры, связанные с этим фильмом.
        saveFilmGenres(film);

        // Возвращаем полностью заполненный объект фильма, включая данные MPA и жанры, загруженные по его новому ID.
        return getFilmById(filmId);
    }

    @Override
    public Film updateFilm(Film film) {
        // Выполняем предварительную валидацию объекта Film.
        if (!FilmValidate.validateFilm(film)) {
            throw new IllegalArgumentException("Film validation failed");
        }

        // Проверяем существование фильма по его ID. Если фильма нет, getFilmById выбросит NotFoundException.
        getFilmById(film.getId());

        // Проверяем существование MPA-рейтинга. Если MPA указан, но не найден в базе, выбрасываем исключение.
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            // Получаем MPA-рейтинг по ID. Если он не найден, будет выброшено исключение NotFoundException
            // внутри метода getMpaById.
            mpaRatingStorage.getMpaById(film.getMpa().getId());
        }

        // SQL-запрос для обновления существующего фильма.
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        int rowsAffected = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() == null ? null : film.getMpa().getId(), // Учитываем, что mpa может быть null
                film.getId());

        // Если rowsAffected равно 0, значит фильм не был найден (хотя getFilmById должен был это поймать раньше).
        // Это может указывать на гонку или несогласованность данных.
        if (rowsAffected == 0) {
            throw new NotFoundException("Film not found with ID: " + film.getId());
        }

        // Обновляем жанры фильма (удаляем старые и добавляем новые).
        updateFilmGenres(film);

        // Возвращаем полностью заполненный объект фильма после обновления.
        return getFilmById(film.getId());
    }

    @Override
    public List<Film> getAllFilms() {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name AS mpa_name " +
                "FROM films AS f JOIN mpa_ratings AS mr ON f.mpa_id = mr.mpa_id";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper());
        films.forEach(film -> film.setGenres(getFilmGenres(film.getId())));
        return films;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        // Проверяем существование фильма и пользователя перед добавлением лайка
        getFilmById(filmId); // Проверит фильм
        // Предполагается, что UserStorage есть и его можно использовать для проверки пользователя.
        // Если UserStorage нет, то эта проверка должна быть добавлена.
        // userStorage.getUserById(userId).orElseThrow(() -> new NotFoundException("User not found with ID: " + userId));

        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, filmId, userId);
        } catch (DataIntegrityViolationException e) {
            // Обработка случая, если лайк уже существует или user_id/film_id не существуют (если настроены FK)
            // Здесь можно выбросить кастомное исключение или проигнорировать, если это допустимо.
            // Например: throw new IllegalArgumentException("Like already exists or invalid film/user ID.");
            // Или если FK настроены: throw new NotFoundException("Film or User not found.");
            System.out.println("Like already exists or invalid film/user ID: " + e.getMessage()); // Для отладки
        }
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        // Проверяем существование фильма (пользователя можно не проверять, если удаляем)
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
                "JOIN mpa_ratings AS mr ON f.mpa_id = mr.mpa_id " + // Присоединяем mpa_ratings
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name " + // Добавляем все поля для GROUP BY
                "ORDER BY COUNT(l.user_id) DESC " +
                "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper(), count);
        films.forEach(film -> film.setGenres(getFilmGenres(film.getId()))); // Загружаем жанры
        return films;
    }

    @Override
    public Film getFilmById(Long filmId) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, mr.name AS mpa_name " +
                "FROM films AS f JOIN mpa_ratings AS mr ON f.mpa_id = mr.mpa_id WHERE f.film_id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, filmRowMapper(), filmId);
            film.setGenres(getFilmGenres(filmId)); // Загружаем жанры
            return film;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException("Film not found with ID: " + filmId);
        }
    }

    private RowMapper<Film> filmRowMapper() {
        return (rs, rowNum) -> Film.builder()
                .id(rs.getLong("film_id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getInt("duration"))
                .mpa(new MpaRating(rs.getLong("mpa_id"), rs.getString("mpa_name"))) // Предполагаем int для mpa_id
                .build();
    }

    // --- Методы для работы с жанрами фильма ---

    // Пример кода из предыдущего ответа, адаптированный под текущий контекст
// Он предпочтительнее вашего текущего for-each с отдельными INSERT запросами.
    private void saveFilmGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            film.setGenres(List.of());
            return;
        }

        List<Genre> distinctAndSortedGenres = film.getGenres().stream()
                .filter(g -> g.getId() != null)
                .distinct() // Использует equals/hashCode Genre, или убедитесь, что они уникальны по ID.
                .sorted(Comparator.comparing(Genre::getId)) // Сортируем для предсказуемости
                .collect(Collectors.toList());

        if (distinctAndSortedGenres.isEmpty()) {
            film.setGenres(List.of());
            return;
        }

        String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)"; // Исправлено на film_genres
        try {
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Genre genre = distinctAndSortedGenres.get(i);
                    // Проверяем существование жанра перед вставкой, N+1 проблема здесь.
                    genreStorage.getGenreById(genre.getId())
                            .orElseThrow(() -> new NotFoundException("Genre not found with ID: " + genre.getId()));
                    ps.setLong(1, film.getId());
                    ps.setLong(2, genre.getId());
                }

                @Override
                public int getBatchSize() {
                    return distinctAndSortedGenres.size();
                }
            });
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("Failed to save film genres for film ID: " + film.getId(), e);
        }
        film.setGenres(getFilmGenres(film.getId())); // Перечитываем для актуальности
    }


    // Обновляет жанры для существующего фильма (удаляет все старые и добавляет новые)
    private void updateFilmGenres(Film film) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId()); // Удаляем все старые связи

        saveFilmGenres(film); // Добавляем новые
    }

    // Получает список жанров для указанного фильма
    private List<Genre> getFilmGenres(Long filmId) {
        String sql = "SELECT fg.genre_id, g.name AS genre_name FROM film_genres AS fg " +
                "JOIN genres AS g ON fg.genre_id = g.genre_id WHERE fg.film_id = ? ORDER BY fg.genre_id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            return new Genre(rs.getLong("genre_id"), rs.getString("genre_name"));
        }, filmId);
    }
}




