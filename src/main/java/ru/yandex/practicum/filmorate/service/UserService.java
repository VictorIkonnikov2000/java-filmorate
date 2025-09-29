// UserService.java
package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    // Внедрение зависимости от хранилища пользователей, используя qualifier для выбора конкретной реализации
    private final @Qualifier("UserDbStorage") UserStorage userStorage;

    /**
     * Создает нового пользователя.
     * Перед сохранением пользователя в хранилище, выполняется его валидация.
     * Если имя пользователя пустое, оно заменяется логином.
     *
     * @param user Пользователь для создания.
     * @return Созданный пользователь с присвоенным ID.
     */
    public User createUser(User user) {
        UserValidate.validateUser(user); // Применяем валидацию
        // Если имя пользователя не задано, используем логин в качестве имени
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.createUser(user);
    }

    /**
     * Обновляет существующего пользователя.
     * Перед обновлением пользователя в хранилище, выполняется его валидация.
     * Если имя пользователя пустое, оно заменяется логином.
     *
     * @param user Пользователь для обновления (должен содержать ID).
     * @return Обновленный пользователь.
     */
    public User updateUser(User user) {
        UserValidate.validateUser(user); // Применяем валидацию
        // Если имя пользователя не задано, используем логин в качестве имени
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.updateUser(user);
    }

    /**
     * Возвращает список всех пользователей.
     *
     * @return Список объектов User.
     */
    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    /**
     * Добавляет пользователя в друзья к другому пользователю.
     *
     * @param userId   ID пользователя, который добавляет друга.
     * @param friendId ID пользователя, которого добавляют в друзья.
     */
    public void addFriend(Long userId, Long friendId) {
        userStorage.addFriend(userId, friendId);
    }

    /**
     * Удаляет пользователя из друзей.
     *
     * @param userId   ID пользователя, который удаляет друга.
     * @param friendId ID пользователя, которого удаляют из друзей.
     */
    public void removeFriend(Long userId, Long friendId) {
        userStorage.removeFriend(userId, friendId);
    }

    /**
     * Возвращает список друзей для указанного пользователя.
     *
     * @param id ID пользователя, чьих друзей нужно найти.
     * @return Список объектов User, представляющих друзей пользователя.
     */
    public List<User> getFriends(Long id) {
        return userStorage.getFriends(id);
    }

    /**
     * Возвращает список общих друзей между двумя пользователями.
     *
     * @param userId  ID первого пользователя.
     * @param otherId ID второго пользователя.
     * @return Список объектов User, представляющих общих друзей.
     */
    public List<User> getCommonFriends(Long userId, Long otherId) {
        // Получаем списки друзей для обоих пользователей
        List<User> userFriends = userStorage.getFriends(userId);
        List<User> otherFriends = userStorage.getFriends(otherId);

        // Если у одного из пользователей нет друзей, возвращаем пустой список
        if (userFriends == null || userFriends.isEmpty() || otherFriends == null || otherFriends.isEmpty()) {
            return Collections.emptyList();
        }

        // Создаем множество ID друзей первого пользователя для быстрого поиска
        Set<Long> userFriendIds = userFriends.stream()
                .filter(Objects::nonNull) // Фильтруем на случай null-объектов в списке
                .map(User::getId)
                .collect(Collectors.toSet());

        // Фильтруем друзей второго пользователя, оставляя только тех, чьи ID есть в множестве первого
        return otherFriends.stream()
                .filter(Objects::nonNull) // Фильтруем на случай null-объектов в списке
                .filter(friend -> userFriendIds.contains(friend.getId()))
                .collect(Collectors.toList());
    }

    /**
     * Возвращает пользователя по его ID.
     *
     * @param id ID пользователя.
     * @return Объект User, соответствующий указанному ID.
     */
    public User getUserById(Long id) {
        return userStorage.getUserById(id);
    }
}



