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
import java.sql.Timestamp;
import java.time.Instant;
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

        // Проверяем, существует ли уже запрос на дружбу в любом направлении.
        String checkRequestSql = "SELECT COUNT(*) FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        Integer existingRequestCount = jdbcTemplate.queryForObject(checkRequestSql, Integer.class, userId, friendId, friendId, userId);

        if (existingRequestCount != null && existingRequestCount > 0) {
            log.warn("Попытка добавить существующий запрос на дружбу между {} и {}. Операция не требуется.", userId, friendId);
            return; // Запрос уже существует, ничего не делаем.
        }

        // Отправляем запрос на дружбу.  initiator_id - инициатор запроса (тот, кто отправляет запрос в друзья).
        String insertSql = "INSERT INTO friends (user_id, friend_id, requested_at, initiator_id) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(insertSql, userId, friendId, Timestamp.from(Instant.now()), userId);

        log.info("Пользователь {} отправил запрос на дружбу пользователю {}.", userId, friendId);
    }

    /**
     * Подтверждение запроса на добавление в друзья.
     */
    public void confirmFriend(Long userId, Long friendId) {
        getUserById(userId); // Проверка существования пользователя
        getUserById(friendId); // Проверка существования друга

        // Проверяем, существует ли запрос на дружбу от userId к friendId.
        String checkRequestSql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ? AND accepted_at IS NULL";
        Integer existingRequestCount = jdbcTemplate.queryForObject(checkRequestSql, Integer.class, userId, friendId);

        if (existingRequestCount == null || existingRequestCount == 0) {
            log.warn("Не найден запрос на дружбу от пользователя {} к пользователю {}. Подтверждение невозможно.", userId, friendId);
            throw new NotFoundException("Не найден запрос на дружбу от пользователя " + userId + " к пользователю " + friendId + ". Подтверждение невозможно.");

        }

        // Подтверждаем запрос на дружбу, устанавливая accepted_at.
        String updateSql = "UPDATE friends SET accepted_at = ? WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(updateSql, Timestamp.from(Instant.now()), userId, friendId);

        log.info("Пользователь {} подтвердил запрос на дружбу от пользователя {}.", friendId, userId);
    }

    /**
     * Удаление из друзей (удаление запроса или факта дружбы).
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может сам себя удалить из друзей.");
        }
        getUserById(userId);
        getUserById(friendId);

        // Проверяем, есть ли вообще какие-либо записи о дружбе между пользователями в любом направлении.

        String checkFriendshipSql = "SELECT COUNT(*) FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        Integer existingFriendshipCount = jdbcTemplate.queryForObject(checkFriendshipSql, Integer.class, userId, friendId, friendId, userId);

        if (existingFriendshipCount == null || existingFriendshipCount == 0) {
            log.warn("Между пользователями {} и {} нет записей о дружбе. Удаление невозможно.", userId, friendId);
            return; // Нет записей о дружбе, удалять нечего.
        }

        // Пытаемся удалить запись, где userId инициировал запрос к friendId.
        String deleteSql = "DELETE FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";

        int deletedRows = jdbcTemplate.update(deleteSql, userId, friendId, friendId ,userId);

        log.info("Удалена дружба (или запрос) между пользователями {} и {}. Удалено {} записей.", userId, friendId, deletedRows);
    }

    /**
     * Получить список друзей пользователя (только подтвержденных).
     */
    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Проверить существование пользователя

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "INNER JOIN friends AS f ON u.user_id = f.friend_id " + // Находим всех, кто является другом для user_id
                "WHERE f.user_id = ? AND f.accepted_at IS NOT NULL"; // Фильтруем только подтвержденные дружбы

        return jdbcTemplate.query(sql, userRowMapper(), userId);
    }

    /**
     * Получить список общих друзей между двумя пользователями.
     */
    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "INNER JOIN friends AS f1 ON u.user_id = f1.friend_id AND f1.accepted_at IS NOT NULL " +
                "INNER JOIN friends AS f2 ON u.user_id = f2.friend_id AND f2.accepted_at IS NOT NULL " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";

        return jdbcTemplate.query(sql, userRowMapper(), userId, otherUserId);
    }

    /**
     * Получить список друзей друзей пользователя.
     */
    @Override
    public List<User> getFriendsOfFriends(Long userId) {
        getUserById(userId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "INNER JOIN friends AS f ON u.user_id = f.friend_id AND f.accepted_at IS NOT NULL " +
                "WHERE f.user_id IN (SELECT f2.friend_id FROM friends AS f2 WHERE f2.user_id = ? AND f2.accepted_at IS NOT NULL) " +
                "AND u.user_id <> ? " +
                "AND u.user_id NOT IN (SELECT f3.friend_id FROM friends AS f3 WHERE f3.user_id = ? AND f3.accepted_at IS NOT NULL)";

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId, userId);
    }
}









