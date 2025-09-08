package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    private final Map<Long, Set<Long>> friends = new HashMap<>();
    private Long userIdCounter = 1L;


    @Override
    public ResponseEntity<?> createUser(User user) {
        try {
            UserValidate.validateUser(user);
            user.setId(userIdCounter++);
            users.put(user.getId(), user);
            log.info("Создан пользователь: {}", user);
            return new ResponseEntity<>(user, HttpStatus.CREATED);
        } catch (ValidationException e) {
            log.error("Ошибка валидации при создании пользователя: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<?> updateUser(User user) {
        try {
            UserValidate.validateUser(user);
            if (!users.containsKey(user.getId())) {
                log.warn("Пользователь с id {} не найден.", user.getId());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Пользователь не найден");
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
            users.put(user.getId(), user);
            log.info("Обновляем пользователя: {}", user);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (ValidationException e) {
            log.error("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Запрос на получение списка всех пользователей.");
        return new ResponseEntity<>(new ArrayList<>(users.values()), HttpStatus.OK);
    }


    @Override
    public void addFriend(Long userId, Long friendId) {
        if (!users.containsKey(userId) || !users.containsKey(friendId)) {
            log.warn("Попытка добавить в друзья несуществующего пользователя.");
            return;
        }

        friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        friends.computeIfAbsent(friendId, k -> new HashSet<>()).add(userId);
        log.info("Пользователи {} и {} теперь друзья.", userId, friendId);
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (friends.containsKey(userId)) {
            friends.get(userId).remove(friendId);
        }
        if (friends.containsKey(friendId)) {
            friends.get(friendId).remove(userId);
        }
        log.info("Пользователи {} и {} больше не друзья.", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        Set<Long> friendIds = friends.getOrDefault(userId, Collections.emptySet());
        return friendIds.stream()
                .map(friendId -> {
                    User user = users.get(friendId);
                    return user;
                })
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
            throw new UserNotFoundException("Пользователь с id " + id + " не найден."); // Выбрасываем исключение, если пользователь не найден
        }
        return user;
    }
}
