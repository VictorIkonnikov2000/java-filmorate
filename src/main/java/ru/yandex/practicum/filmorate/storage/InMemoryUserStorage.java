package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException; // Добавим для обработки случая "дружба с собой"
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Component("InMemoryUserStorage")
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new HashMap<>();
    // Переменная 'friends' будет хранить односторонние связи.
    // Для симметричной дружбы мы будем добавлять записи в оба направления.
    private final Map<Long, Set<Long>> friends = new HashMap<>(); // userId -> Set<friendId>
    private Long userIdCounter = 1L;


    @Override
    public User createUser(User user) {
        UserValidate.validateUser(user); // Валидация пользователя
        if (user.getId() != null && users.containsKey(user.getId())) {
            throw new ValidationException("Пользователь с таким ID уже существует.");
        }
        user.setId(userIdCounter++); // Присваиваем id
        users.put(user.getId(), user); // Сохраняем пользователя
        log.info("Создан пользователь: {}", user);
        return user; // Возвращаем созданного пользователя
    }

    @Override
    public User updateUser(User user) {
        UserValidate.validateUser(user); // Валидация пользователя
        if (user.getId() == null) {
            throw new ValidationException("ID пользователя не может быть пустым для обновления.");
        }
        if (!users.containsKey(user.getId())) {
            log.warn("Пользователь с id {} не найден для обновления.", user.getId());
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
        // Проверяем существование обоих пользователей
        if (!users.containsKey(userId)) {
            log.warn("Пользователь с id {} не найден при попытке добавить друга.", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        if (!users.containsKey(friendId)) {
            log.warn("Пользователь (друг) с id {} не найден при попытке добавить друга.", friendId);
            throw new NotFoundException("Пользователь с id " + friendId + " не найден.");
        }
        // Проверяем, что пользователи не пытаются добавить сами себя в друзья
        if (userId.equals(friendId)) {
            log.warn("Пользователь {} пытался добавить сам себя в друзья.", userId);
            throw new ValidationException("Пользователь не может добавить самого себя в друзья.");
        }

        // Добавляем user2 в друзья user1 (делаем дружбу симметричной)
        friends.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        // Добавляем user1 в друзья user2
        friends.computeIfAbsent(friendId, k -> new HashSet<>()).add(userId);

        log.info("Пользователь {} добавил в друзья пользователя {} (и наоборот).", userId, friendId);
    }

    @Override
    public boolean removeFriend(Long userId, Long friendId) {
        // Проверяем существование пользователя, который удаляет друга
        if (!users.containsKey(userId)) {
            log.warn("Пользователь с id {} не найден при попытке удалить друга.", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        // Проверяем существование пользователя, которого удаляют из друзей
        // Если friendId не существует, все равно кидаем NotFound, согласно тестам 08 и 09
        if (!users.containsKey(friendId)) {
            log.warn("Пользователь (друг) с id {} не найден при попытке удалить друга.", friendId);
            throw new NotFoundException("Пользователь с id " + friendId + " не найден.");
        }

        boolean removedFromUser = false;
        boolean removedFromFriend = false;

        // Удаляем друга из списка друзей userId
        if (friends.containsKey(userId)) {
            removedFromUser = friends.get(userId).remove(friendId);
        }
        // Удаляем userId из списка друзей friendId (для симметричности)
        if (friends.containsKey(friendId)) {
            removedFromFriend = friends.get(friendId).remove(userId);
        }

        // Если хотя бы одна связь была удалена, считаем операцию успешной.
        boolean removed = removedFromUser || removedFromFriend;
        log.info("Пользователь {} попытался удалить из друзей пользователя {}. Успешно: {}. (Обратная связь: {})", userId, friendId, removedFromUser, removedFromFriend);
        return removed;
    }


    /**
     * Возвращает список друзей для указанного пользователя.
     * Поскольку дружба теперь симметрична, достаточно проверить только прямые связи.
     *
     * @param userId ID пользователя, чьих друзей нужно получить.
     * @return Список объектов User, представляющих друзей.
     * @throws NotFoundException Если пользователь с указанным ID не найден.
     */
    @Override
    public List<User> getFriends(Long userId) {
        if (!users.containsKey(userId)) {
            log.warn("Пользователь с id {} не найден при запросе списка друзей.", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        // Получаем идентификаторы друзей пользователя
        Set<Long> friendIds = friends.getOrDefault(userId, Collections.emptySet());
        // Преобразуем идентификаторы в объекты User
        return friendIds.stream()
                .map(users::get) // Получаем User по id
                .filter(Objects::nonNull) // Отфильтровываем null, если вдруг кто-то удалил пользователя, но ссылка осталась
                .collect(Collectors.toList());
    }

    /**
     * Возвращает список общих друзей между двумя пользователями.
     *
     * @param userId ID первого пользователя.
     * @param otherId ID второго пользователя.
     * @return Список объектов User, представляющих общих друзей.
     * @throws NotFoundException Если один из пользователей не найден.
     */
    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        // Проверяем существование обоих пользователей
        if (!users.containsKey(userId)) {
            log.warn("Пользователь с id {} не найден при запросе общих друзей.", userId);
            throw new NotFoundException("Пользователь с id " + userId + " не найден.");
        }
        if (!users.containsKey(otherId)) {
            log.warn("Пользователь с id {} не найден при запросе общих друзей.", otherId);
            throw new NotFoundException("Пользователь с id " + otherId + " не найден.");
        }

        // Получаем множества друзей для каждого пользователя
        Set<Long> userFriends = friends.getOrDefault(userId, Collections.emptySet());
        Set<Long> otherFriends = friends.getOrDefault(otherId, Collections.emptySet());

        // Находим пересечение множеств (общих друзей)
        Set<Long> commonFriendIds = new HashSet<>(userFriends);
        commonFriendIds.retainAll(otherFriends); // Оставляет только те элементы, которые есть в обоих множествах

        // Преобразуем идентификаторы общих друзей в объекты User
        return commonFriendIds.stream()
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
