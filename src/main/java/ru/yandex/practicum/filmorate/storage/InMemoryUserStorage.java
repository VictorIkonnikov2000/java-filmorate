package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component("InMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friends = new HashMap<>();
    private Long userIdCounter = 1L;


    @Override
    public User createUser(User user) {
        UserValidate.validateUser(user); // Валидация пользователя
        user.setId(userIdCounter++); // Присваиваем id
        users.put(user.getId(), user); // Сохраняем пользователя
        log.info("Создан пользователь: {}", user);
        return user; // Возвращаем созданного пользователя
    }

    @Override
    public User updateUser(User user) {
        UserValidate.validateUser(user); // Валидация пользователя
        if (!users.containsKey(user.getId())) {
            log.warn("Пользователь с id {} не найден.", user.getId());
            throw new NotFoundException("Пользователь не найден"); // Ошибка, если пользователя нет
        }
        users.put(user.getId(), user); // Обновляем пользователя
        log.info("Обновляем пользователя: {}", user);
        return user; // Возвращаем обновленного пользователя
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Запрос на получение списка всех пользователей.");
        return new ArrayList<>(users.values()); // Возвращаем список всех пользователей
    }


    @Override
    public void addFriend(Long userId, Long friendId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        if (!users.containsKey(friendId)) {
            throw new NotFoundException("Пользователь с id " + friendId + " не найден.");
        }
        // Теперь дружба односторонняя: добавляем friendId в список друзей userId, не добавляя userId в список friendId.
        friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        log.info("Пользователь {} добавил в друзья пользователя {}.", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        if (!users.containsKey(friendId)) {
            throw new NotFoundException("Пользователь с id " + friendId + " не найден.");
        }
        //Удаляем друга только из списка друзей userId (т.к. дружба односторонняя)
        if (friends.containsKey(userId)) {
            friends.get(userId).remove(friendId);
        }
        log.info("Пользователь {} удалил из друзей пользователя {}.", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        Set<Long> friendIds = friends.getOrDefault(userId, Collections.emptySet());
        return friendIds.stream()
                .map(friendId -> users.get(friendId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        Set<Long> userFriends = friends.getOrDefault(userId, Collections.emptySet());
        Set<Long> otherFriends = friends.getOrDefault(otherId, Collections.emptySet());

        Set<Long> commonFriends = new HashSet<>(userFriends);
        commonFriends.retainAll(otherFriends);
        return commonFriends.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public User getUserById(Long id) {
        User user = users.get(id);
        if (user == null) {
            log.warn("Пользователь с id {} не найден.", id);
            throw new NotFoundException("Пользователь с id " + id + " не найден."); // Выбрасываем исключение, если пользователь не найден
        }
        return user;
    }
}