package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component("UserDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Возвращает RowMapper для преобразования ResultSet в объект User.
     */
    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }

    @Override
    public User createUser(User user) {
        UserValidate.validateUser(user);
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            log.error("Не удалось получить сгенерированный ключ для пользователя.");
            throw new RuntimeException("Не удалось получить идентификатор пользователя после создания.");
        }
        user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        log.info("Создан пользователь: {}", user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        UserValidate.validateUser(user);
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        getUserById(user.getId()); // Проверяем, что пользователь существует

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());

        log.info("Обновлен пользователь: {}", user);
        return user;
    }


    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        return jdbcTemplate.query(sql, userRowMapper());
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Пользователь с id {} не найден.", id);
            throw new NotFoundException("Пользователь с id " + id + " не найден.");
        }
    }

    /**
     * Добавление друга. Теперь дружба хранится как две записи для взаимности.
     */
    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья.");
        }

        getUserById(userId); // Проверка существования пользователя
        getUserById(friendId); // Проверка существования друга

        // Проверяем, существует ли уже дружба в любом направлении (даже если только одна запись).
        // Это предотвращает добавление дубликата, если по какой-то причине одна запись уже есть.
        String checkFriendshipSql = "SELECT COUNT(*) FROM friends WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        Integer existingFriendshipCount = jdbcTemplate.queryForObject(checkFriendshipSql, Integer.class, userId, friendId, friendId, userId);

        if (existingFriendshipCount != null && existingFriendshipCount > 0) {
            log.warn("Попытка добавить существующую дружбу между {} и {}. Операция не требуется.", userId, friendId);
            return; // Дружба уже существует, ничего не делаем.
        }

        // Добавляем две записи о дружбе, что подразумевает взаимное согласие сразу
        String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, true)";
        jdbcTemplate.update(insertSql, userId, friendId);
        jdbcTemplate.update(insertSql, friendId, userId); // Добавление обратной записи для взаимности

        log.info("Пользователи {} и {} теперь друзья.", userId, friendId);
    }

    /**
     * Удаление друга. Удаляет обе записи о взаимной дружбе.
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может сам себя удалить из друзей.");
        }
        getUserById(userId);
        getUserById(friendId);

        // Удаляем обе записи о взаимной дружбе
        String deleteSql = "DELETE FROM friends WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        int deletedRows = jdbcTemplate.update(deleteSql, userId, friendId, friendId, userId);

        if (deletedRows == 0) { // Если не было удалено ни одной записи
            log.warn("Не найдено дружбы между пользователем {} и {}.", userId, friendId);
        } else { // Если удалена как минимум одна запись (в идеале две)
            log.info("Взаимная дружба между пользователем {} и {} разорвана. Удалено {} записей.", userId, friendId, deletedRows);
        }
    }


    /**
     * Получить список друзей пользователя.
     * Поскольку дружба хранится как две записи (user1_id -> user2_id и user2_id -> user1_id),
     * достаточно выбрать user2_id, где user1_id соответствует заданному userId, или user1_id, где user2_id соответствует заданному userId.
     * Использование DISTINCT в SELECT помогает избежать дублирования, если по какой-то причине будет найдено более одной записи для одного и того же друга.
     */
    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Проверить существование пользователя

        String sql = "SELECT DISTINCT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "INNER JOIN friends AS f ON " +
                "  (u.user_id = f.user2_id AND f.user1_id = ?) OR " + // Если user_id - это друг (user2_id) для данного userId (user1_id)
                "  (u.user_id = f.user1_id AND f.user2_id = ?) " +   // Или user_id - это друг (user1_id) для данного userId (user2_id)
                "WHERE f.status = TRUE"; // Учитываем только подтвержденные дружбы (поскольку ваш addFriend сразу делает status = true)

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId);
    }


    /**
     * Получить список общих друзей между двумя пользователями.
     * Общие друзья - это те, кто является другом и для userId, и для otherUserId.
     */
    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        // Этот запрос находит пользователей, которые являются друзьями для обоих userId и otherUserId.
        // Используем два подзапроса JOIN, чтобы найти друзей для каждого пользователя,
        // затем пересекаем их, чтобы найти общих.
        String sql = "SELECT DISTINCT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "INNER JOIN friends AS f1 ON (u.user_id = f1.user2_id AND f1.user1_id = ?) OR (u.user_id = f1.user1_id AND f1.user2_id = ?) " +
                "INNER JOIN friends AS f2 ON (u.user_id = f2.user2_id AND f2.user1_id = ?) OR (u.user_id = f2.user1_id AND f2.user2_id = ?) " +
                "WHERE f1.status = TRUE AND f2.status = TRUE"; // Учитываем только подтвержденные дружбы

        return jdbcTemplate.query(sql, userRowMapper(),
                userId, userId, // Параметры для f1: друга userId
                otherUserId, otherUserId); // Параметры для f2: друга otherUserId
    }


    /**
     * Получить список друзей друзей пользователя.
     * Исключаются сам пользователь и его прямые друзья.
     */
    @Override
    public List<User> getFriendsOfFriends(Long userId) {
        getUserById(userId);

        // SQL для поиска друзей друзей:
        // 1. Найти всех прямых друзей пользователя (f1).
        // 2. Для каждого прямого друга (f1) найти его друзей (f2).
        // 3. Результаты из (2) - это друзья друзей.
        // 4. Исключить самого пользователя (u.user_id = userId).
        // 5. Исключить прямых друзей пользователя (тот же список, что в (1)).
        String sql = "SELECT DISTINCT fof.user_id, fof.email, fof.login, fof.name, fof.birthday " +
                "FROM users AS u " + // Сам пользователь
                "INNER JOIN friends AS uf ON (u.user_id = uf.user1_id AND uf.user2_id = ?) OR (u.user_id = uf.user2_id AND uf.user1_id = ?) " + // Дружба пользователя с его прямыми друзьями
                "INNER JOIN users AS direct_friend ON direct_friend.user_id = CASE WHEN uf.user1_id = ? THEN uf.user2_id ELSE uf.user1_id END " + // Прямой друг
                "INNER JOIN friends AS df_fof ON (direct_friend.user_id = df_fof.user1_id AND df_fof.user2_id = fof.user_id) OR (direct_friend.user_id = df_fof.user2_id AND df_fof.user1_id = fof.user_id) " + // Дружба прямого друга с другом друга
                "INNER JOIN users AS fof ON fof.user_id = CASE WHEN df_fof.user1_id = direct_friend.user_id THEN df_fof.user2_id ELSE df_fof.user1_id END " + // Друг друга
                "WHERE uf.status = TRUE AND df_fof.status = TRUE " + // Учитываем только подтвержденные дружбы
                "AND fof.user_id <> ? " + // Исключаем самого пользователя
                "AND fof.user_id NOT IN (" + // И исключаем прямых друзей пользователя
                "    SELECT CASE WHEN f_dir.user1_id = ? THEN f_dir.user2_id ELSE f_dir.user1_id END " +
                "    FROM friends AS f_dir " +
                "    WHERE (f_dir.user1_id = ? OR f_dir.user2_id = ?) AND f_dir.status = TRUE" +
                ")";

        // Параметры для sql:
        // 1. uf.user2_id = ? -> userId
        // 2. uf.user1_id = ? -> userId
        // 3. direct_friend.user_id = CASE WHEN uf.user1_id = ? -> userId
        // 4. fof.user_id <> ? -> userId
        // 5. f_dir.user1_id = ? -> userId (для подзапроса исключения прямых друзей)
        // 6. f_dir.user1_id = ? -> userId (для подзапроса)
        // 7. f_dir.user2_id = ? -> userId (для подзапроса)
        return jdbcTemplate.query(sql, userRowMapper(),
                userId, userId, userId, // Для поиска прямых друзей и определения direct_friend
                userId, // Для исключения самого пользователя
                userId, userId, userId); // Для исключения прямых друзей
    }
}









