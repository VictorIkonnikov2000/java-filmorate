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
        return jdbcTemplate.query(sql, genreRowMapper());
    }

    @Override
    public Optional<Genre> getGenreById(Long id) {
        String sql = "SELECT genre_id, name FROM genres WHERE genre_id = ?";
        try {
            Genre genre = jdbcTemplate.queryForObject(sql, genreRowMapper(), id);
            return Optional.ofNullable(genre);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }


    private RowMapper<Genre> genreRowMapper() {
        return (rs, rowNum) -> new Genre(rs.getLong("genre_id"), rs.getString("name"));
    }

    @Override
    public Genre addGenre(Genre genre) {
        String sql = "INSERT INTO genres (name) VALUES (?)";
        jdbcTemplate.update(sql, genre.getName());
        // Получаем id добавленной записи. В данном случае для простоты полагаем, что id генерируется последовательно
        Long id = jdbcTemplate.queryForObject("SELECT MAX(genre_id) FROM genres", Long.class);
        genre.setId(id);
        return genre;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        String sql = "UPDATE genres SET name = ? WHERE genre_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, genre.getName(), genre.getId());
        if (rowsAffected == 0) {
            return null; // жанр не найден
        }
        return genre;
    }

    @Override
    public void deleteGenre(Long id) {
        String sql =  "DELETE FROM genres WHERE genre_id = ?";
        jdbcTemplate.update(sql, id);
    }
}
