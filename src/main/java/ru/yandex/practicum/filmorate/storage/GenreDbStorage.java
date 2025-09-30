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
            log.info("Таблица genres пуста, начинаем добавление стандартных жанров."); // Более точное сообщение
            addInitialGenre(new Genre(1L, "Комедия"));
            addInitialGenre(new Genre(2L, "Драма"));
            addInitialGenre(new Genre(3L, "Мультфильм"));
            addInitialGenre(new Genre(4L, "Триллер"));
            addInitialGenre(new Genre(5L, "Документальный"));
            addInitialGenre(new Genre(6L, "Боевик"));
            log.info("Стандартные жанры успешно добавлены.");
        } else {
            // Проверяем, что все стандартные жанры присутствуют, а не просто "что-то есть"
            // Это может быть overkill для простой инициализации, но полезно для надежности.
            // Если вы уверены, что после первой инициализации таблица всегда полная,
            // то можно оставить просто log.info("Жанры уже существуют...");
            log.info("Жанры уже существуют в базе данных, проверка и добавление пропущены.");
        }
    }

    private void addInitialGenre(Genre genre) {
        // Изменяем SQL-запрос для использования столбца 'id' вместо 'genre_id'
        String sql = "INSERT INTO genres (id, name) VALUES (?, ?)";

        try {
            jdbcTemplate.update(sql, genre.getId(), genre.getName());
            log.info("Жанр добавлен: {}", genre.getName());
        } catch (DuplicateKeyException e) {
            // Если такой жанр уже существует (по ID, если он PRIMARY KEY),
            // ловим исключение. Сообщение в логе теперь более явное.
            log.warn("Жанр с ID {} ({}) уже существует в базе данных.", genre.getId(), genre.getName());
        } catch (Exception e) {
            log.error("Ошибка при добавлении жанра {}: {}", genre.getName(), e.getMessage());
        }
    }

    @Override
    public List<Genre> getAllGenres() {
        // Исправлено: имя столбца genre_id на id
        String sql = "SELECT id, name FROM genres ORDER BY id";
        return jdbcTemplate.query(sql, genreRowMapper());
    }

    @Override
    public Optional<Genre> getGenreById(Long id) {
        // Исправлено: имя столбца genre_id на id
        String sql = "SELECT id, name FROM genres WHERE id = ?";
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
     * Исправлено: получение ID из столбца 'id'
     * @return RowMapper<Genre>
     */
    private RowMapper<Genre> genreRowMapper() {
        // Исправлено: получение ID из столбца 'id' вместо 'genre_id'
        return (rs, rowNum) -> new Genre(rs.getLong("id"), rs.getString("name"));
    }

    // Метод addGenre уже был корректен, так как он использует автогенерацию ID.
    // Однако, если вы хотите явно указывать ID при добавлении (как в addInitialGenre),
    // то SQL-запрос должен быть INSERT INTO genres (id, name) ...
    // В текущей реализации addGenre, ID генерируется базой данных.
    @Override
    public Genre addGenre(Genre genre) {
        String sql = "INSERT INTO genres (name) VALUES (?)"; // ID будет сгенерирован автоматически
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            // Исправлено: Указываем, что генерируемый ключ - это "id" (если это требуется для вашей БД)
            // Для H2 и большинства БД, если столбец автоинкрементный и первичный ключ,
            // Statement.RETURN_GENERATED_KEYS обычно достаточно.
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, genre.getName());
            return ps;
        }, keyHolder);

        Long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        genre.setId(id);
        log.info("Добавлен новый жанр: {} с ID: {}", genre.getName(), genre.getId());
        return genre;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        // Исправлено: имя столбца genre_id на id
        String sql = "UPDATE genres SET name = ? WHERE id = ?";
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
        // Исправлено: имя столбца genre_id на id
        String sql = "DELETE FROM genres WHERE id = ?";
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

        // Преобразуем Long в Integer, если id в БД int (для H2 это обычно так).
        // Если 'id' в БД LONG, то можно оставить Long.
        // Я оставляю как было, предполагая, что ID у вас может быть Long.
        List<Long> longGenreIds = new ArrayList<>(genreIds);

        String inSql = String.join(",", Collections.nCopies(longGenreIds.size(), "?"));
        // Исправлено: имя столбца genre_id на id
        String sql = String.format("SELECT id, name FROM genres WHERE id IN (%s) ORDER BY id ASC", inSql);

        // Передаем список ID как массив для аргументов
        return jdbcTemplate.query(sql, longGenreIds.toArray(), this::mapRowToGenre);
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        // Исправлено: получение ID из столбца 'id' и имени из столбца 'name'
        return Genre.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name")) // Предполагается, что столбец с именем называется 'name' не 'genre_name'
                .build();
    }
}




