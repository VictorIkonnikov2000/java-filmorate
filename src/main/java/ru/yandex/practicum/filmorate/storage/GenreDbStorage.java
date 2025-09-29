package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

@Component("GenreDbStorage")
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public GenreDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Genre> getAllGenres() {
        String sql = "SELECT genre_id, name FROM genres";
        return jdbcTemplate.query(sql, genreRowMapper()); // Используем RowMapper для преобразования строк БД в объекты Genre
    }

    @Override
    public Optional<Genre> getGenreById(Long id) {
        String sql = "SELECT genre_id, name FROM genres WHERE genre_id = ?";
        try {
            Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper(), id); // Получаем один объект
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty(); // Если жанр не найден, возвращаем пустой Optional
        }
    }


    private RowMapper<Genre> genreRowMapper() {
        return (rs, rowNum) -> new Genre(rs.getLong("genre_id"), rs.getString("name")); // Используем конструктор, требующий id и name
    }

    @Override
    public Genre addGenre(Genre genre) {
        //TODO: implementation
        return null;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        //TODO: implementation
        return null;
    }

    @Override
    public void deleteGenre(Long id) {
        //TODO: implementation
    }
}
