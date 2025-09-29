

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
import ru.yandex.practicum.filmorate.validate.UserValidate; // Убедитесь, что этот класс корректно валидирует

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects; // Для работы с GeneratedKeyHolder

@Component("UserDbStorage") // Убедитесь, что бин называется "UserDbStorage"
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User createUser(User user) {
        // Проверка валидности пользователя перед добавлением в базу данных
        // Эта часть критична для первого набора ошибок.
        // Убедитесь, что UserValidate.validateUser корректно обрабатывает входные данные из тестов.
        if (!UserValidate.validateUser(user)) {
            // Если валидация не пройдена, выбрасываем исключение
            throw new ValidationException("User validation failed");
        }

        // Если имя пользователя не задано, используем логин в качестве имени
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        // Для получения сгенерированного id используем KeyHolder
        KeyHolder keyHolder = new GeneratedKeyHolder();

        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"}); // Указываем, что ожидаем сгенерированный "user_id"
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        // Получаем сгенерированный ID и устанавливаем его для объекта пользователя
        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        user.setId(generatedId);

        return user; // Возвращаем пользователя с присвоенным ID
    }

    @Override
    public User updateUser(User user) {
        // Также необходимо убедиться, что пользователь валиден при обновлении
        if (!UserValidate.validateUser(user)) {
            throw new ValidationException("User validation failed");
        }
        // Если имя пользователя не задано, используем логин в качестве имени
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        // Проверяем, существует ли пользователь перед обновлением
        // Если user.getId() null, это может привести к ошибке.
        // Убедитесь, что объект пользователя, переданный сюда, имеет id.
        if (user.getId() == null) {
            throw new ValidationException("User ID is required for update operation");
        }
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());
        if (rows == 0) {
            // Если ни одна строка не была обновлена, значит пользователь не найден
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
        // Проверка на добавление самого себя в друзья
        if (userId.equals(friendId)) {
            throw new ValidationException("Cannot add self as friend.");
        }
        // Проверка, что user_id и friend_id существуют
        getUserById(userId); // Выбросит NotFoundException если нет
        getUserById(friendId); // Выбросит NotFoundException если нет

        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, false)"; // Изначально статус false (не подтверждена)
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId, friendId);
        if (rowsAffected == 0) {
            // Если ничего не удалено, значит не было такой дружбы
            throw new NotFoundException("Friendship not found or already removed.");
        }
    }

    @Override
    public List<User> getFriends(Long userId) {
        // Проверка, что пользователь существует
        getUserById(userId); // Выбросит NotFoundException если нет

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday FROM users u " +
                "JOIN friends f ON u.user_id = f.friend_id WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper(), userId);
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        // Проверка, что оба пользователя существуют
        getUserById(userId);
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
        } catch (org.springframework.dao.DataRetrievalFailureException | org.springframework.dao.IncorrectResultSizeDataAccessException e) {
            // Логгирование ошибки будет полезно
            // log.warn("User with id {} not found", id);
            throw new NotFoundException("User not found with id: " + id);
        }
    }

    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday") != null ? rs.getDate("birthday").toLocalDate() : null) // Обработка null даты
                .build();
    }
}


