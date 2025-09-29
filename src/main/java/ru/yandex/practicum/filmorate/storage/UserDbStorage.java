package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.List;
import java.util.Objects;
// Добавим логгер для вывода информации об ошибке, если нужно
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

@Component("UserDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    // private static final Logger log = LoggerFactory.getLogger(UserDbStorage.class); // Инициализация логгера

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User createUser(User user) {
        if (!UserValidate.validateUser(user)) {
            throw new ValidationException("User validation failed");
        }

        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"});
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        user.setId(generatedId);

        return user;
    }

    @Override
    public User updateUser(User user) {
        if (!UserValidate.validateUser(user)) {
            throw new ValidationException("User validation failed");
        }
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        if (user.getId() == null) {
            throw new ValidationException("User ID is required for update operation");
        }
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());
        if (rows == 0) {
            throw new NotFoundException("User not found");
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        return jdbcTemplate.query(sql, userRowMapper());
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Cannot add self as friend.");
        }
        getUserById(userId);
        getUserById(friendId);

        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId, friendId);
        if (rowsAffected == 0) {
            throw new NotFoundException("Friendship not found or already removed.");
        }
    }

    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Проверяем существование пользователя

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday FROM users u " +
                "JOIN friends f ON u.user_id = f.friend_id WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper(), userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        getUserById(userId); // Проверяем существование обоих пользователей
        getUserById(otherId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday FROM users u " +
                "WHERE u.user_id IN (SELECT friend_id FROM friends WHERE user_id = ?) " +
                "AND u.user_id IN (SELECT friend_id FROM friends WHERE user_id = ?)";
        return jdbcTemplate.query(sql, userRowMapper(), userId, otherId);
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper(), id);
        } catch (org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            // Это исключение выбрасывается, когда queryForObject ожидает одну строку,
            // а получает 0 или более одной. Чаще всего 0, если пользователь не найден.
            // log.warn("User with id {} not found or multiple users found: {}", id, e.getMessage()); // Пример логгирования
            throw new NotFoundException("User not found with id: " + id);
        } catch (org.springframework.dao.DataRetrievalFailureException e) {
            // Это более общее исключение для ошибок извлечения данных.
            // Включает IncorrectResultSizeDataAccessException, но так как оно уже поймано выше,
            // здесь будут обрабатываться другие связанные с извлечением данных ошибки.
            // log.error("Data retrieval error for user with id {}: {}", id, e.getMessage()); // Пример логгирования
            throw new NotFoundException("Failed to retrieve user data for id: " + id + ". Details: " + e.getMessage());
        }
    }

    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday") != null ? rs.getDate("birthday").toLocalDate() : null)
                .build();
    }
}


