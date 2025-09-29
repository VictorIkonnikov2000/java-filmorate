package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

@Component
public class MpaRaitingDbStorage implements MpaRatingStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaRaitingDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MpaRating getMpaRatingById(int id) {
        // Этот метод устарел, чтобы не сломать совместимость выбрасываем исключение
        throw new UnsupportedOperationException("Этот метод устарел. Используйте getMpaById(Long id)");
    }

    @Override
    public List<MpaRating> getAllMpa() {
        String sql = "SELECT mpa_id, name FROM mpa";
        return jdbcTemplate.query(sql, mpaRowMapper());// Используем RowMapper
    }

    @Override
    public MpaRating getMpaById(Long id) {
        String sql = "SELECT mpa_id, name FROM mpa WHERE mpa_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, mpaRowMapper(), id); // Получаем один объект
        } catch (EmptyResultDataAccessException e) {
            return null; // Возвращаем null, если рейтинг не найден
        }
    }

    private RowMapper<MpaRating> mpaRowMapper() {
        return (rs, rowNum) -> {
            // Используем конструктор MpaRating(Long id, String name)
            return new MpaRating(rs.getLong("mpa_id"), rs.getString("name"));
        };
    }
}
