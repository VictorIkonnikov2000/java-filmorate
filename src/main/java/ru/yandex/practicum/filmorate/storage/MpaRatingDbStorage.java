package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;


// Адаптируем интерфейс MpaRatingStorage, если он не был Optional
// public interface MpaRatingStorage {
//     List<MpaRating> getAllMpaRatings();
//     Optional<MpaRating> getMpaById(Long id); // Возвращаем Optional
// }

@Component("MpaRatingDbStorage")
public class MpaRatingDbStorage implements MpaRatingStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaRatingDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Сохраняем getMpaRatingById(int id) для совместимости, но рекомендуем использовать Long
    @Override
    public MpaRating getMpaRatingById(int id) {
        // Вызываем основной метод, который теперь тоже возвращает MpaRating напрямую.
        // Если getMpaById выбрасывает NotFoundException, то и этот метод его пробросит.
        return getMpaById((long) id);
    }

    @Override
    public List<MpaRating> getAllMpa() { // Переименовал в getAllMpa, чтобы избежать путаницы с методами на интерфейсе.
        String sql = "SELECT mpa_id, name FROM mpa_ratings"; // Предполагается, что таблица называется mpa_ratings
        return jdbcTemplate.query(sql, mpaRowMapper());
    }

    @Override
    public MpaRating getMpaById(Long id) { // Теперь возвращает MpaRating напрямую
        String sql = "SELECT mpa_id, name FROM mpa_ratings WHERE mpa_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, mpaRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            // Если рейтинг не найден, выбрасываем собственное исключение,
            // чтобы вызывающий код мог явно это обработать.
            throw new NotFoundException("MPA rating with id " + id + " not found.");
        }
    }

    private RowMapper<MpaRating> mpaRowMapper() {
        return (rs, rowNum) -> {
            return new MpaRating(rs.getLong("mpa_id"), rs.getString("name"));
        };
    }
}

