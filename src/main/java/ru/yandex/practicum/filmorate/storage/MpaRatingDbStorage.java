package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

@Slf4j
@Component("MpaRatingDbStorage")
public class MpaRatingDbStorage implements MpaRatingStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public MpaRatingDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initializeMpaRatingsIfEmpty() {
        log.info("Инициализация стандартных MPA-рейтингов в базе данных...");
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM mpa_ratings", Integer.class);

        if (count == null || count == 0) {
            log.info("Таблица mpa_ratings пуста, начинаем добавление стандартных рейтингов.");
            addInitialMpaRating(new MpaRating(1L, "G"));
            addInitialMpaRating(new MpaRating(2L, "PG"));
            addInitialMpaRating(new MpaRating(3L, "PG-13"));
            addInitialMpaRating(new MpaRating(4L, "R"));
            addInitialMpaRating(new MpaRating(5L, "NC-17"));
            log.info("Стандартные MPA-рейтинги успешно добавлены.");
        } else {
            log.info("MPA-рейтинги уже существуют в базе данных, инициализация пропущена.");
        }
    }

    private void addInitialMpaRating(MpaRating mpaRating) {
        String sql = "INSERT INTO mpa_ratings (mpa_id, name) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, mpaRating.getId(), mpaRating.getName());
            log.info("Инициализирующий MPA-рейтинг добавлен: {} (ID: {})", mpaRating.getName(), mpaRating.getId());
        } catch (DuplicateKeyException e) {
            log.warn("MPA-рейтинг с ID {} уже существует в базе данных: {}", mpaRating.getId(), mpaRating.getName());
        } catch (Exception e) {
            log.error("Ошибка при добавлении инициализирующего MPA-рейтинга {} (ID: {}): {}",
                    mpaRating.getName(), mpaRating.getId(), e.getMessage());
        }
    }


    @Override
    public MpaRating getMpaRatingById(int id) {
        return getMpaById((long) id);
    }


    @Override
    public List<MpaRating> getAllMpa() {
        String sql = "SELECT mpa_id, name FROM mpa_ratings ORDER BY mpa_id";
        return jdbcTemplate.query(sql, mpaRowMapper());
    }


    @Override
    public MpaRating getMpaById(Long id) {
        String sql = "SELECT mpa_id, name FROM mpa_ratings WHERE mpa_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, mpaRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            log.warn("MPA rating with id {} not found in the database.", id);
            throw new NotFoundException("MPA rating with id " + id + " not found.");
        }
    }


    private RowMapper<MpaRating> mpaRowMapper() {
        return (rs, rowNum) -> new MpaRating(rs.getLong("mpa_id"), rs.getString("name"));
    }
}






