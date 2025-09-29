package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.util.*;
import java.util.stream.Collectors;

@Component("InMemoryUserStorage")
@Slf4j
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
        // Проверка существования обоих пользователей перед добавлением дружбы.
        // Вызов getUserById() обработает выброс NotFoundException, если какого-то пользователя нет.
        getUserById(userId);
        getUserById(friendId);

        // Получаем или создаем набор друзей для userId и добавляем friendId.
        friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        log.info("Пользователь {} добавил в друзья пользователя {}.", userId, friendId);
    }

    /**
     * Удаляет друга для указанного пользователя.
     * При этом дружба считается односторонней: удаляется только связь userId -> friendId.
     * Для соответствия требованиям тестов (AssertionError expected 404),
     * метод теперь возвращает boolean, указывающий, была ли дружба успешно удалена (т.е. существовала).
     *
     * @param userId   ID пользователя, который удаляет друга.
     * @param friendId ID пользователя, которого удаляют из друзей.
     * @return true, если дружба была найдена и удалена; false, если дружба не существовала.
     * @throws NotFoundException Если userId или friendId не существуют.
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        // Проверка существования обоих пользователей перед удалением дружбы.
        // Вызов getUserById() обработает выброс NotFoundException, если какого-то пользователя нет.
        getUserById(userId);
        getUserById(friendId);

        Set<Long> userFriends = friends.get(userId);
        if (userFriends != null) {
            // Удаляем друга. Метод remove возвращает true, если элемент присутствовал и был удален.
            boolean removed = userFriends.remove(friendId);
            if (removed) {
                log.info("Пользователь {} удалил из друзей пользователя {}.", userId, friendId);
            } else {
                log.warn("Попытка удаления: Дружба между пользователем {} и {} не найдена.", userId, friendId);
            }

        } else {
            // У пользователя userId нет списка друзей, значит друг friendId не мог быть там.
            log.warn("Попытка удаления: У пользователя {} нет списка друзей, дружба с {} не найдена.", userId, friendId);

        }
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

    @Override
    public void confirmFriendship(Long userId, Long friendId) {
        
    }
}