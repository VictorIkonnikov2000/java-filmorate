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
import ru.yandex.practicum.filmorate.validate.UserValidate; // Предполагается, что это статический метод или синглтон

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
        UserValidate.validateUser(user); // Вызываем статический метод валидации
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
        UserValidate.validateUser(user); // Вызываем статический метод валидации
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        getUserById(user.getId()); // Проверяем, что пользователь существует

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int updatedRows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());

        if (updatedRows == 0) {
            log.error("Пользователь с ID {} не найден для обновления.", user.getId());
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден.");
        }

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
            throw new ValidationException("Пользователь не может добавить себя в друзья.");
        }
        getUserById(userId); // Check for existence
        getUserById(friendId); // Check for existence

        String insertSql = "INSERT INTO friends (user1_id, user2_id) VALUES (?, ?)";  // Simplified: no status
        try {
            jdbcTemplate.update(insertSql, userId, friendId);
            log.info("Пользователь {} добавил в друзья пользователя {}.", userId, friendId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("Пользователь {} уже добавил в друзья пользователя {}.", userId, friendId); // Or handle as needed (e.g., update status if you decide to use status later)
        }


    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может удалить себя из друзей.");
        }
        getUserById(userId);
        getUserById(friendId);

        String deleteSql = "DELETE FROM friends WHERE user1_id = ? AND user2_id = ?";
        int deletedRows = jdbcTemplate.update(deleteSql, userId, friendId);

        if (deletedRows == 0) {
            log.warn("Пользователь {} не имеет в друзьях пользователя {}.", userId, friendId);
        } else {
            log.info("Пользователя {} удалил из друзей пользователя {}. Удалена {} записей.", userId, friendId, deletedRows);
        }
    }


    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Check for existence

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f ON u.user_id = f.user2_id " +
                "WHERE f.user1_id = ?"; // Только тех, кого userId добавил
        return jdbcTemplate.query(sql, userRowMapper(), userId);
    }


    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        // Находим пересечение множеств друзей userId и otherUserId
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "WHERE u.user_id IN (SELECT f.user2_id FROM friends f WHERE f.user1_id = ?) " +
                "AND u.user_id IN (SELECT f.user2_id FROM friends f WHERE f.user1_id = ?)";

        return jdbcTemplate.query(sql, userRowMapper(), userId, otherUserId);
    }

}












