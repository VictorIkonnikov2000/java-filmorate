package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Импортируем Slf4j для логирования
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException; // Импортируем кастомное исключение NotFoundException
import ru.yandex.practicum.filmorate.exception.ValidationException; // Импортируем кастомное исключение ValidationException
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

import static ru.yandex.practicum.filmorate.validate.UserValidate.validateUser;

@Service
@RequiredArgsConstructor
@Slf4j // Добавляем аннотацию для использования логирования
public class UserService {

    // Внедряем зависимость UserStorage, используя квалификатор для выбора конкретной реализации
    private final @Qualifier("UserDbStorage") UserStorage userStorage;

    /**
     * Создает нового пользователя.
     * Выполняет валидацию пользователя перед сохранением.
     *
     * @param user Объект пользователя для создания.
     * @return Созданный объект пользователя с присвоенным ID.
     */
    public User createUser(User user) {
        validateUser(user); // Выполняем предварительную валидацию полей пользователя
        return userStorage.createUser(user); // Передаем пользователя в хранилище для сохранения
    }

    /**
     * Обновляет существующего пользователя.
     * Выполняет валидацию пользователя и проверяет его существование перед обновлением.
     *
     * @param user Объект пользователя для обновления. Должен содержать ID существующего пользователя.
     * @return Обновленный объект пользователя.
     * @throws NotFoundException Если пользователь с указанным ID не найден.
     */
    public User updateUser(User user) {
        validateUser(user); // Выполняем предварительную валидацию полей пользователя
        // Проверяем, существует ли пользователь с таким ID в хранилище перед обновлением
        if (userStorage.getUserById(user.getId()) == null) {
            log.warn("Попытка обновить несуществующего пользователя с ID: {}", user.getId());
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден.");
        }
        return userStorage.updateUser(user); // Передаем пользователя в хранилище для обновления
    }

    /**
     * Возвращает список всех зарегистрированных пользователей.
     *
     * @return Список объектов User.
     */
    public List<User> getAllUsers() {
        return userStorage.getAllUsers(); // Получаем и возвращаем список всех пользователей из хранилища
    }

    /**
     * Добавляет пользователя с userId в друзья к пользователю с friendId и наоборот.
     * Проверяет, что пользователи существуют и не являются одним и тем же лицом.
     *
     * @param userId   ID пользователя, который добавляет в друзья.
     * @param friendId ID пользователя, которого добавляют в друзья.
     * @throws ValidationException Если userId и friendId совпадают.
     * @throws NotFoundException   Если один из пользователей не найден.
     */
    public void addFriend(Long userId, Long friendId) {
        // Проверяем, что пользователь не пытается добавить сам себя в друзья
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался добавить сам себя в друзья.", userId);
            throw new ValidationException("Пользователь не может добавить сам себя в друзья.");
        }

        // Проверяем существование обоих пользователей по их ID
        User user = userStorage.getUserById(userId);
        if (user == null) {
            log.warn("Пользователь с ID {} не найден при попытке добавления в друзья.", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден.");
        }
        User friend = userStorage.getUserById(friendId);
        if (friend == null) {
            log.warn("Пользователь-друг с ID {} не найден при попытке добавления в друзья.", friendId);
            throw new NotFoundException("Пользователь (друг) с ID " + friendId + " не найден.");
        }

        // Передаем запрос на добавление друга в хранилище
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} отправил запрос в друзья пользователю {}.", userId, friendId);
    }

    /**
     * Удаляет пользователя с friendId из списка друзей пользователя с userId.
     * Проверяет существование пользователей.
     *
     * @param userId   ID пользователя, который удаляет из друзей.
     * @param friendId ID пользователя, которого удаляют из друзей.
     * @throws ValidationException Если userId и friendId совпадают.
     * @throws NotFoundException   Если один из пользователей не найден или дружба не существует.
     */
    public void removeFriend(Long userId, Long friendId) {
        // Проверяем, что пользователь не пытается удалить сам себя из друзей
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался удалить сам себя из друзей.", userId);
            throw new ValidationException("Пользователь не может удалить сам себя из друзей.");
        }

        // Проверяем существование обоих пользователей по их ID
        User user = userStorage.getUserById(userId);
        if (user == null) {
            log.warn("Пользователь с ID {} не найден при попытке удаления из друзей.", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден.");
        }
        User friend = userStorage.getUserById(friendId);
        if (friend == null) {
            log.warn("Пользователь-друг с ID {} не найден при попытке удаления из друзей.", friendId);
            throw new NotFoundException("Пользователь (друг) с ID " + friendId + " не найден.");
        }

        // Вызываем метод хранилища для удаления друга.
        // Поскольку userStorage.removeFriend теперь void, нам не нужно проверять boolean-результат.
        // Если метод удаляется или вызывает исключение, это будет обработано ниже по стеку.
        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}.", userId, friendId);
    }

    /**
     * Возвращает список друзей пользователя с указанным ID.
     *
     * @param id ID пользователя, чьих друзей необходимо получить.
     * @return Список объектов User, представляющих друзей пользователя.
     * @throws NotFoundException Если пользователь с указанным ID не найден.
     */
    public List<User> getFriends(Long id) {
        // Проверяем существование пользователя, чьих друзей нужно получить
        if (userStorage.getUserById(id) == null) {
            log.warn("Пользователь с ID {} не найден при попытке получить список друзей.", id);
            throw new NotFoundException("Пользователь с ID " + id + " не найден.");
        }
        return userStorage.getFriends(id); // Получаем и возвращаем список друзей пользователя из хранилища
    }

    /**
     * Возвращает список общих друзей между двумя пользователями.
     *
     * @param userId  ID первого пользователя.
     * @param otherId ID второго пользователя.
     * @return Список объектов User, представляющих общих друзей. Возвращает пустой список, если общих друзей нет.
     * @throws NotFoundException Если один из пользователей не найден.
     */
    public List<User> getCommonFriends(Long userId, Long otherId) {
        // Проверяем существование первого пользователя
        if (userStorage.getUserById(userId) == null) {
            log.warn("Пользователь с ID {} не найден при поиске общих друзей.", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден.");
        }
        // Проверяем существование второго пользователя
        if (userStorage.getUserById(otherId) == null) {
            log.warn("Другой пользователь с ID {} не найден при поиске общих друзей.", otherId);
            throw new NotFoundException("Пользователь с ID " + otherId + " не найден.");
        }

        // Получаем списки друзей для обоих пользователей
        List<User> userFriends = getFriends(userId);
        List<User> otherFriends = getFriends(otherId);

        // Если у одного из пользователей нет друзей, то общих друзей быть не может
        if (userFriends.isEmpty() || otherFriends.isEmpty()) {
            return Collections.emptyList(); // Возвращаем пустой список
        }

        // Преобразуем список друзей первого пользователя в Set ID для быстрого поиска
        Set<Long> userFriendIds = userFriends.stream()
                .filter(Objects::nonNull) // Фильтруем null-объекты, если такие могут появиться
                .map(User::getId)         // Извлекаем ID каждого друга
                .collect(Collectors.toSet());

        // Фильтруем друзей второго пользователя, оставляя только тех, чьи ID присутствуют в Set первого пользователя
        return otherFriends.stream()
                .filter(Objects::nonNull) // Фильтруем null-объекты
                .filter(friend -> userFriendIds.contains(friend.getId())) // Проверяем наличие ID друга в Set
                .collect(Collectors.toList()); // Собираем результат в список
    }

    /**
     * Возвращает пользователя по его ID.
     *
     * @param id ID пользователя.
     * @return Объект User, если пользователь найден.
     * @throws NotFoundException Если пользователь с указанным ID не найден.
     */
    public User getUserById(Long id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            log.warn("Пользователь с ID {} не найден.", id);
            throw new NotFoundException("Пользователь с ID " + id + " не найден.");
        }
        return user; // Возвращаем найденного пользователя
    }
}


