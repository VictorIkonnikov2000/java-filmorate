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

    private final  UserStorage userStorage;


    public User createUser(User user) {
        validateUser(user);
        log.info("Создание пользователя: {}", user.getLogin());
        return userStorage.createUser(user);
    }


    public User updateUser(User user) {
        validateUser(user);
        // Проверка существования пользователя теперь инкапсулирована в userStorage.updateUser
        // userStorage.updateUser сам вызовет getUserByIdOrThrow(user.getId()) и выбросит NotFoundException
        // если пользователь не найден, или если update-запрос не затронул ни одной строки.
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
        // Проверка существования пользователей теперь инкапсулирована в userStorage.addFriend
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} отправил запрос в друзья пользователю {}.", userId, friendId);
    }



    public void acceptFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался принять запрос от самого себя.", userId);
            throw new ValidationException("Невозможно подтвердить запрос от самого себя.");
        }
        // userStorage.confirmFriendship теперь сам проверяет существование пользователей и наличие заявки
        userStorage.confirmFriendship(userId, friendId);
        log.info("Пользователь {} принял заявку в друзья от пользователя {}.", userId, friendId);
    }


    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался удалить сам себя из друзей.", userId);
            throw new ValidationException("Пользователь не может удалить сам себя из друзей.");
        }
        // Проверка существования пользователей и дружбы теперь инкапсулирована в userStorage.removeFriend
        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}.", userId, friendId);
    }


    public List<User> getFriends(Long id) {
        // Проверка существования пользователя теперь инкапсулирована в userStorage.getFriends
        log.info("Получение списка друзей для пользователя с ID: {}", id);
        return userStorage.getFriends(id);
    }


    public List<User> getCommonFriends(Long userId, Long otherId) {
        // Проверка существования пользователей теперь инкапсулирована в userStorage.getCommonFriends
        log.info("Получение списка общих друзей для пользователей {} и {}.", userId, otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }


    public User getUserById(Long id) {
        // userStorage.getUserById теперь сам выбрасывает NotFoundException если пользователь не найден.
        User user = userStorage.getUserById(id);
        log.info("Получение пользователя по ID: {}", id);
        return user;
    }
}


