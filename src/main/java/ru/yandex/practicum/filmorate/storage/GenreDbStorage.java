package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component("GenreDbStorage")
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initializeGenresIfEmpty() {
        log.info("Инициализация стандартных жанров в базе данных...");
        List<Genre> genres = getAllGenres();
        if (genres.isEmpty()) {
            addInitialGenre(new Genre(1L, "Комедия"));
            addInitialGenre(new Genre(2L, "Драма"));
            addInitialGenre(new Genre(3L, "Мультфильм"));
            addInitialGenre(new Genre(4L, "Триллер"));
            addInitialGenre(new Genre(5L, "Документальный"));
            addInitialGenre(new Genre(6L, "Боевик"));
            log.info("Стандартные жанры успешно добавлены.");
        } else {
            log.info("Жанры уже существуют в базе данных, инициализация пропущена.");
        }
    }

    private void addInitialGenre(Genre genre) {
        // Изменяем SQL-запрос для совместимости с H2.
        // H2 не поддерживает ON CONFLICT DO NOTHING напрямую.
        // Вместо этого мы можем использовать INSERT IGNORE (с определенными ограничениями)
        // или, что более универсально, отловить исключение DuplicateKeyException,
        // если жанр с таким ID уже существует (предполагая, что genre_id - PRIMARY KEY).
        // Или, как вариант, проверить существование перед вставкой.
        // Для простоты и учитывая, что инициализация происходит один раз,
        // мы можем попробовать вставить и отловить исключение, если такая запись уже есть.

        // SQL-запрос для H2, который будет вызывать ошибку, если запись уже существует
        String sql = "INSERT INTO genres (genre_id, name) VALUES (?, ?)";

        try {
            jdbcTemplate.update(sql, genre.getId(), genre.getName());
            log.info("Жанр добавлен: {}", genre.getName());
        } catch (DuplicateKeyException e) {
            // Если такой жанр уже существует (по genre_id - если он PRIMARY KEY),
            // ловим исключение и игнорируем его, так как цель - не добавлять дубликаты.
            log.warn("Жанр с ID {} уже существует: {}", genre.getId(), genre.getName());
        } catch (Exception e) {
            log.error("Ошибка при добавлении жанра {}: {}", genre.getName(), e.getMessage());
        }
    }

    @Override
    public List<Genre> getAllGenres() {
        String sql = "SELECT genre_id, name FROM genres ORDER BY genre_id";
        return jdbcTemplate.query(sql, genreRowMapper());
    }

    @Override
    public Optional<Genre> getGenreById(Long id) {
        String sql = "SELECT genre_id, name FROM genres WHERE genre_id = ?";
        try {
            Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper(), id);
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Жанр с ID {} не найден в базе данных.", id);
            return Optional.empty();
        }
    }

    /**
     * Создает RowMapper для преобразования ResultSet в объект Genre.
     * @return RowMapper<Genre>
     */
    private RowMapper<Genre> genreRowMapper() {
        return (rs, rowNum) -> new Genre(rs.getLong("genre_id"), rs.getString("name"));
    }

    @Override
    public Genre addGenre(Genre genre) {
        String sql = "INSERT INTO genres (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, genre.getName());
            return ps;
        }, keyHolder);

        // Получаем сгенерированный ID и устанавливаем его в объект Genre
        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        genre.setId(id);
        log.info("Добавлен новый жанр: {} с ID: {}", genre.getName(), genre.getId());
        return genre;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        String sql = "UPDATE genres SET name = ? WHERE genre_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, genre.getName(), genre.getId());
        if (rowsAffected == 0) {
            log.error("Попытка обновления несуществующего жанра с ID: {}", genre.getId());
            throw new NotFoundException("Genre not found with ID: " + genre.getId());
        }
        log.info("Обновлен жанр: {} с ID: {}", genre.getName(), genre.getId());
        return genre;
    }

    @Override
    public void deleteGenre(Long id) {
        String sql = "DELETE FROM genres WHERE genre_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        if (rowsAffected == 0) {
            log.error("Попытка удаления несуществующего жанра с ID: {}", id);
            throw new NotFoundException("Genre not found with ID: " + id);
        }
        log.info("Удален жанр с ID: {}", id);
    }

    @Override
    public List<Genre> getGenresByIds(Collection<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Преобразуем Long в Integer, если genre_id в БД int.
        // Или можно использовать Object[] и H2 преобразует автоматически, но лучше явно.
        List<Integer> intGenreIds = genreIds.stream()
                .map(Long::intValue)
                .collect(Collectors.toList());

        // Формируем строку с плейсхолдерами (?, ?, ...) для IN-клаузы
        String inSql = String.join(",", Collections.nCopies(intGenreIds.size(), "?"));
        String sql = String.format("SELECT * FROM genres WHERE genre_id IN (%s) ORDER BY genre_id ASC", inSql);

        // Передаем список ID как массив для аргументов
        return jdbcTemplate.query(sql, intGenreIds.toArray(), this::mapRowToGenre);
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        return Genre.builder()
                .id(rs.getLong("genre_id"))
                .name(rs.getString("genre_name"))
                .build();
    }
}



