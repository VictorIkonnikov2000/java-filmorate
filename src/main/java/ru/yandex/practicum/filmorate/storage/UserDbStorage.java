package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;


import ru.yandex.practicum.filmorate.validate.UserValidate;
import java.util.List;
import java.sql.Date;

@Component
@Qualifier("userDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public User createUser(User user) {
        if (!UserValidate.validateUser(user)) {
            throw new ValidationException("User validation failed"); // выбрасываем исключение при неудачной валидации
        }
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()));
        return user; // возвращаем созданного пользователя
    }

    @Override
    public User updateUser(User user) {
        //TODO: Implementation
        return null;
    }

    @Override
    public List<User> getAllUsers() {
        //TODO: Implementation
        return null;
    }

    @Override
    public void addFriend(Long userId, Long friendId) {
        //TODO: Implementation
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        //TODO: Implementation
    }

    @Override
    public List<User> getFriends(Long id) {
        //TODO: Implementation
        return null;
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        //TODO: Implementation
        return null;
    }

    @Override
    public User getUserById(Long id) {
        //TODO: Implementation
        return null;
    }
}

