package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j; // Используем Slf4j для логирования
import org.springframework.beans.factory.annotation.Autowired; // Для автоматического внедрения зависимостей
import org.springframework.dao.DuplicateKeyException; // Для обработки случаев, когда элемент с таким ID уже есть
import org.springframework.dao.EmptyResultDataAccessException; // Для обработки случаев, когда запрос не возвращает строк
import org.springframework.jdbc.core.JdbcTemplate; // Компонент Spring для работы с базой данных
import org.springframework.jdbc.core.RowMapper; // Интерфейс для маппинга строк результата запроса в объекты
import org.springframework.stereotype.Component; // Аннотация, указывающая, что класс является компонентом Spring
import ru.yandex.practicum.filmorate.exception.NotFoundException; // Кастомное исключение для ненаходимых объектов
import ru.yandex.practicum.filmorate.model.MpaRating; // Модель данных для MPA рейтинга

import java.util.List; // Для работы со списками объектов

@Slf4j // Аннотация Lombok для автоматического создания логгера
@Component("MpaRatingDbStorage") // Компонент Spring с указанием имени
public class MpaRatingDbStorage implements MpaRatingStorage {

    private final JdbcTemplate jdbcTemplate; // Поле для работы с базой данных

    // Конструктор с автоматическим внедрением JdbcTemplate
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
        // Проверяем количество записей в таблице mpa_ratings
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM mpa_ratings", Integer.class);

        // Если таблица пуста (count равен null или 0), то добавляем стандартные рейтинги
        if (count == null || count == 0) {
            log.info("Таблица mpa_ratings пуста, начинаем добавление стандартных рейтингов.");
            // Добавляем каждый MPA-рейтинг с явно указанным ID
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
        String sql = "INSERT INTO mpa_ratings (mpa_id, name) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, mpaRating.getId(), mpaRating.getName());
            log.info("Инициализирующий MPA-рейтинг добавлен: {} (ID: {})", mpaRating.getName(), mpaRating.getId());
        } catch (DuplicateKeyException e) {
            // Если рейтинг с таким ID уже существует, логируем предупреждение
            log.warn("MPA-рейтинг с ID {} уже существует в базе данных: {}", mpaRating.getId(), mpaRating.getName());
        } catch (Exception e) {
            // Логируем любые другие ошибки при вставке
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
        // Делегируем вызов другому методу, который работает с Long ID и выбрасывает исключение при ненаходке
        return getMpaById((long) id);
    }

    /**
     * Возвращает список всех MPA-рейтингов, хранящихся в базе данных.
     * @return Список объектов MpaRating.
     */
    @Override
    public List<MpaRating> getAllMpa() {
        // SQL-запрос для получения всех MPA-рейтингов, отсортированных по их идентификатору.
        String sql = "SELECT mpa_id, name FROM mpa_ratings ORDER BY mpa_id";
        // Выполняем запрос и маппим каждую строку результата в объект MpaRating
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
        String sql = "SELECT mpa_id, name FROM mpa_ratings WHERE mpa_id = ?";
        try {
            // Выполняем запрос, ожидая один объект MpaRating.
            // Если объект не найден, queryForObject выбрасывает EmptyResultDataAccessException.
            return jdbcTemplate.queryForObject(sql, mpaRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            // В случае отсутствия MPA-рейтинга, логируем предупреждение и выбрасываем кастомное исключение.
            log.warn("MPA rating with id {} not found in the database.", id);
            throw new NotFoundException("MPA rating with id " + id + " not found.");
        }
    }

    /**
     * Создает RowMapper, который преобразует строку из ResultSet в объект MpaRating.
     * @return Экземпляр RowMapper<MpaRating>.
     */
    private RowMapper<MpaRating> mpaRowMapper() {
        // Лямбда-выражение для RowMapper, которое считывает mpa_id и name из ResultSet
        return (rs, rowNum) -> new MpaRating(rs.getLong("mpa_id"), rs.getString("name"));
    }
}


