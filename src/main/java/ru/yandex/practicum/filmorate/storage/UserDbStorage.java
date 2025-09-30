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

    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья.");
        }

        getUserById(userId);
        getUserById(friendId);

        // Проверяем, существует ли уже дружба в любом порядке.
        // Используем OR для проверки обоих направлений.
        String checkFriendshipSql = "SELECT COUNT(*) FROM friends WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        Integer existingFriendsCount = jdbcTemplate.queryForObject(checkFriendshipSql, Integer.class, userId, friendId, friendId, userId);

        if (existingFriendsCount != null && existingFriendsCount > 0) {
            throw new ValidationException("Дружба между пользователями " + userId + " и " + friendId + " уже установлена.");
        }

        // Добавляем две записи о дружбе со статусом TRUE (взаимная)
        String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, true)";
        jdbcTemplate.update(insertSql, userId, friendId);
        jdbcTemplate.update(insertSql, friendId, userId); // Добавление обратной записи для взаимности

        log.info("Пользователи {} и {} теперь друзья.", userId, friendId);
    }


    // Метод confirmFriendship теперь удален, так как дружба сразу становится подтвержденной.
    // Класс FriendshipStatus также удален.

    /**
     * Удаление друга. Удаляет обе записи о взаимной дружбе.
     */
    /**
     * Удаление друга. Удаляет обе записи о взаимной дружбе.
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может сам себя удалить из друзей.");
        }
        getUserById(userId); // Предполагается, что этот метод выбрасывает NotFoundException, если пользователь не найден
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
     * достаточно выбрать user2_id, где user1_id соответствует заданному userId.
     */
    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Проверить существование пользователя

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "INNER JOIN friends AS f ON u.user_id = f.user2_id " +
                "WHERE f.user1_id = ?"; // Ищем друзей, для которых userId является user1_id

        return jdbcTemplate.query(sql, userRowMapper(), userId);
    }

    /**
     * Получить список общих друзей между двумя пользователями.
     * Общие друзья - это те, кто является другом и для userId, и для otherUserId.
     */
    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f1 ON u.user_id = f1.user2_id " + // u - друг для userId
                "JOIN friends AS f2 ON u.user_id = f2.user2_id " + // u - друг для otherUserId
                "WHERE f1.user1_id = ? AND f2.user1_id = ?";

        return jdbcTemplate.query(sql, userRowMapper(), userId, otherUserId);
    }

    /**
     * Получить список друзей друзей пользователя.
     * Исключаются сам пользователь и его прямые друзья.
     */
    @Override
    public List<User> getFriendsOfFriends(Long userId) {
        getUserById(userId);

        String sql = "SELECT DISTINCT ff.user_id, ff.email, ff.login, ff.name, ff.birthday " +
                "FROM users AS ff " + // Обозначаем "друзей друзей" как ff
                "JOIN friends AS f1 ON ff.user_id = f1.user2_id " + // ff - друг для F1.user1_id
                "JOIN friends AS f2 ON f1.user1_id = f2.user2_id " + // F1.user1_id - друг для userId (то есть, это прямой друг userId)
                "WHERE f2.user1_id = ? " + // userId - наш начальный пользователь
                "AND ff.user_id <> ? " + // Исключаем самого себя
                "AND ff.user_id NOT IN (" + // Исключаем прямых друзей userId
                "    SELECT direct_friend.user2_id FROM friends AS direct_friend WHERE direct_friend.user1_id = ?" +
                ")";

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId, userId);
    }
}









