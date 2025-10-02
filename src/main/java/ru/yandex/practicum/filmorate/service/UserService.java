package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

import static ru.yandex.practicum.filmorate.validate.UserValidate.validateUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserStorage userStorage;


    public User createUser(User user) {
        validateUser(user);
        log.info("Создание пользователя: {}", user.getLogin());
        return userStorage.createUser(user);
    }


    public User updateUser(User user) {
        validateUser(user);
        log.info("Обновление пользователя с ID: {}", user.getId());
        return userStorage.updateUser(user);
    }


    public List<User> getAllUsers() {
        log.info("Получение списка всех пользователей.");
        return userStorage.getAllUsers();
    }


    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался добавить сам себя в друзья.", userId);
            throw new ValidationException("Пользователь не может добавить сам себя в друзья.");
        }
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} отправил запрос в друзья пользователю {}.", userId, friendId);
    }


    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался удалить сам себя из друзей.", userId);
            throw new ValidationException("Пользователь не может удалить сам себя из друзей.");
        }
        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}.", userId, friendId);
    }


    public List<User> getFriends(Long id) {
        log.info("Получение списка друзей для пользователя с ID: {}", id);
        return userStorage.getFriends(id);
    }


    public List<User> getCommonFriends(Long userId, Long otherId) {
        log.info("Получение списка общих друзей для пользователей {} и {}.", userId, otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }


    public User getUserById(Long id) {
        User user = userStorage.getUserById(id);
        log.info("Получение пользователя по ID: {}", id);
        return user;
    }

}


