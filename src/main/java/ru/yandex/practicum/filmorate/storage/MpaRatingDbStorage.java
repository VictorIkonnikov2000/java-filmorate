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

    /**
     * Инициализирует стандартные MPA-рейтинги в базе данных, если таблица пуста.
     * Этот метод должен быть вызван при старте приложения для заполнения справочных данных.
     */
    public void initializeMpaRatingsIfEmpty() {
        log.info("Инициализация стандартных MPA-рейтингов в базе данных...");
        // ВАЖНО: Изменено mpa_ratings на rating_mpa
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM rating_mpa", Integer.class);

        // Если таблица пуста (count равен null или 0), то добавляем стандартные рейтинги
        if (count == null || count == 0) {
            log.info("Таблица rating_mpa пуста, начинаем добавление стандартных рейтингов.");
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

    /**
     * Добавляет один начальный MPA-рейтинг в базу данных, обрабатывая дубликаты.
     * Этот метод предназначен для внутренней логики инициализации.
     * @param mpaRating Объект MpaRating для добавления.
     */
    private void addInitialMpaRating(MpaRating mpaRating) {
        // SQL-запрос для вставки MPA-рейтинга с указанием его ID и имени
        String sql = "INSERT INTO rating_mpa (id, name) VALUES (?, ?)"; // Этот запрос уже был правильным
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

    /**
     * Возвращает MPA-рейтинг по его целочисленному идентификатору.
     * Метод для совместимости с интерфейсом, который мог использоваться ранее.
     * @param id Идентификатор MPA-рейтинга.
     * @return Объект MpaRating.
     * @throws NotFoundException Если MPA-рейтинг с данным ID не найден.
     */
    @Override
    public MpaRating getMpaRatingById(int id) {
        return getMpaById((long) id);
    }

    /**
     * Возвращает список всех MPA-рейтингов, хранящихся в базе данных.
     * @return Список объектов MpaRating.
     */
    @Override
    public List<MpaRating> getAllMpa() {
        // SQL-запрос для получения всех MPA-рейтингов, отсортированных по их идентификатору.
        // ВАЖНО: Изменено mpa_id на id и mpa_ratings на rating_mpa
        String sql = "SELECT id, name FROM rating_mpa ORDER BY id";
        return jdbcTemplate.query(sql, mpaRowMapper());
    }

    /**
     * Возвращает MPA-рейтинг по его идентификатору типа Long.
     * Этот метод является основной точкой получения MPA-рейтинга по ID.
     * @param id Идентификатор MPA-рейтинга.
     * @return Объект MpaRating.
     * @throws NotFoundException Если MPA-рейтинг с данным ID не найден в базе данных.
     */
    @Override
    public MpaRating getMpaById(Long id) {
        // SQL-запрос для получения MPA-рейтинга по его ID
        // ВАЖНО: Изменено mpa_id на id и mpa_ratings на rating_mpa
        String sql = "SELECT id, name FROM rating_mpa WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, mpaRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            log.warn("MPA rating with id {} not found in the database.", id);
            throw new NotFoundException("MPA rating with id " + id + " not found.");
        }
    }

    /**
     * Создает RowMapper, который преобразует строку из ResultSet в объект MpaRating.
     * @return Экземпляр RowMapper<MpaRating>.
     */
    private RowMapper<MpaRating> mpaRowMapper() {
        // Лямбда-выражение для RowMapper, которое считывает id и name из ResultSet
        // ВАЖНО: Изменено mpa_id на id
        return (rs, rowNum) -> new MpaRating(rs.getLong("id"), rs.getString("name"));
    }
}





