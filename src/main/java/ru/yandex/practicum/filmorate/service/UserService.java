package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

import static ru.yandex.practicum.filmorate.validate.UserValidate.validateUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final @Qualifier("UserDbStorage") UserStorage userStorage;

    public User createUser(User user) {
        validateUser(user);
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        validateUser(user);
        if (userStorage.getUserById(user.getId()) == null) {
            log.warn("Попытка обновить несуществующего пользователя с ID: {}", user.getId());
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден.");
        }
        return userStorage.updateUser(user);
    }

    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    /**
     * Добавляет пользователя с userId в друзья к пользователю с friendId и наоборот.
     * Проверяет, что пользователи существуют и не являются одним и тем же лицом.
     * Делает дружбу симметричной.
     *
     * @param userId   ID пользователя, который добавляет в друзья.
     * @param friendId ID пользователя, которого добавляют в друзья.
     * @throws ValidationException Если userId и friendId совпадают.
     * @throws NotFoundException   Если один из пользователей не найден.
     */
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался добавить сам себя в друзья.", userId);
            throw new ValidationException("Пользователь не может добавить сам себя в друзья.");
        }

        User user = getUserById(userId); // Используем getUserById для проверки существования и получения пользователя
        User friend = getUserById(friendId); // Используем getUserById для проверки существования и получения друга

        // Вызываем метод хранилища для добавления дружбы
        // Хранилище должно быть реализовано так, чтобы создавать симметричную дружбу
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} и пользователь {} теперь друзья.", userId, friendId);
    }

    /**
     * Удаляет пользователя с friendId из списка друзей пользователя с userId и наоборот.
     * Проверяет существование пользователей.
     *
     * @param userId   ID пользователя, который удаляет из друзей.
     * @param friendId ID пользователя, которого удаляют из друзей.
     * @throws ValidationException Если userId и friendId совпадают.
     * @throws NotFoundException   Если один из пользователей не найден или дружба не существует.
     */
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался удалить сам себя из друзей.", userId);
            throw new ValidationException("Пользователь не может удалить сам себя из друзей.");
        }

        getUserById(userId); // Просто проверяем существование пользователя
        getUserById(friendId); // Просто проверяем существование друга

        // Вызываем метод хранилища для удаления друга и проверяем результат
        // Теперь userStorage.removeFriend должен возвращать boolean
        boolean removed = userStorage.removeFriend(userId, friendId);
        if (!removed) {
            // Если метод removeFriend вернул false, значит дружба не была найдена или удалена
            log.warn("Дружба между пользователем {} и пользователем {} не найдена или не удалена.", userId, friendId);
            throw new NotFoundException("Дружба между пользователем " + userId + " и пользователем " + friendId + " не найдена.");
        }
        log.info("Пользователь {} удалил из друзей пользователя {}.", userId, friendId);
    }

    public List<User> getFriends(Long id) {
        getUserById(id); // Просто проверяем существование пользователя
        return userStorage.getFriends(id);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        getUserById(userId); // Просто проверяем существование первого пользователя
        getUserById(otherId); // Просто проверяем существование второго пользователя

        List<User> userFriends = getFriends(userId);
        List<User> otherFriends = getFriends(otherId);

        if (userFriends.isEmpty() || otherFriends.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userFriendIds = userFriends.stream()
                .filter(Objects::nonNull)
                .map(User::getId)
                .collect(Collectors.toSet());

        return otherFriends.stream()
                .filter(Objects::nonNull)
                .filter(friend -> userFriendIds.contains(friend.getId()))
                .collect(Collectors.toList());
    }

    // Этот метод был выделен для переиспользования и уменьшения дублирования кода
    public User getUserById(Long id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            log.warn("Пользователь с ID {} не найден.", id);
            throw new NotFoundException("Пользователь с ID " + id + " не найден.");
        }
        return user;
    }
}



