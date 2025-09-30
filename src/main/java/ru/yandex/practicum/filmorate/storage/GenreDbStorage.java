package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

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
            log.info("Таблица genres пуста, начинаем добавление стандартных жанров.");
            // Используем addGenre, чтобы ID генерировался автоматически базой данных
            addGenre(new Genre(null, "Комедия")); // Передаем null для ID
            addGenre(new Genre(null, "Драма"));
            addGenre(new Genre(null, "Мультфильм"));
            addGenre(new Genre(null, "Триллер"));
            addGenre(new Genre(null, "Документальный"));
            addGenre(new Genre(null, "Боевик"));
            log.info("Стандартные жанры успешно добавлены.");
        } else {
            log.info("Жанры уже существуют в базе данных, проверка и добавление пропущены.");
        }
    }

    // Метод addInitialGenre больше не нужен, его функциональность перенесена в initializeGenresIfEmpty
    // и осуществляется через addGenre.

    @Override
    public List<Genre> getAllGenres() {
        // ИСПРАВЛЕНО: используем genre_id
        String sql = "SELECT genre_id, name FROM genres ORDER BY genre_id";
        return jdbcTemplate.query(sql, genreRowMapper());
    }

    @Override
    public Optional<Genre> getGenreById(Long id) {
        // ИСПРАВЛЕНО: используем genre_id
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
     * ИСПРАВЛЕНО: получение ID из столбца 'genre_id'
     *
     * @return RowMapper<Genre>
     */
    private RowMapper<Genre> genreRowMapper() {
        // ИСПРАВЛЕНО: получение ID из столбца 'genre_id'
        return (rs, rowNum) -> new Genre(rs.getLong("genre_id"), rs.getString("name"));
    }

    @Override
    public Genre addGenre(Genre genre) {
        String sql = "INSERT INTO genres (name) VALUES (?)"; // ID будет сгенерирован автоматически
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, genre.getName());
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        genre.setId(generatedId); // Устанавливаем сгенерированный ID в объект
        log.info("Добавлен новый жанр: {} с ID: {}", genre.getName(), genre.getId());
        return genre;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        // ИСПРАВЛЕНО: используем genre_id
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
        // ИСПРАВЛЕНО: используем genre_id
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

        String inSql = String.join(",", Collections.nCopies(genreIds.size(), "?"));
        // ИСПРАВЛЕНО: используем genre_id
        String sql = String.format("SELECT genre_id, name FROM genres WHERE genre_id IN (%s) ORDER BY genre_id ASC", inSql);

        // Передаем список ID как массив для аргументов
        return jdbcTemplate.query(sql, genreIds.toArray(), this::mapRowToGenre);
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        // ИСПРАВЛЕНО: получение ID из столбца 'genre_id'
        return Genre.builder()
                .id(rs.getLong("genre_id"))
                .name(rs.getString("name"))
                .build();
    }
}





