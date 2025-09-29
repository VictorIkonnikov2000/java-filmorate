package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.Objects; // Для future-proofing, хотя stream.filter(Objects::nonNull) в getCommonFriends можно удалить
import java.util.Set;
import java.util.stream.Collectors;

import static ru.yandex.practicum.filmorate.validate.UserValidate.validateUser;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final @Qualifier("UserDbStorage") UserStorage userStorage;

    // Метод getUserByIdOrThrow из UserDbStorage можно перенести сюда или создать аналогичный вспомогательный
    // для использования в UserService, если UserStorage не абстрагирует это для всех реализаций.
    // Однако, так как UserDbStorage уже инкапсулирует эту логику, UserDbStorage.get... сам выкинет исключение.

    /**
     * Создает нового пользователя.
     * Выполняет валидацию пользователя перед сохранением.
     *
     * @param user Объект пользователя для создания.
     * @return Созданный объект пользователя с присвоенным ID.
     */
    public User createUser(User user) {
        validateUser(user);
        log.info("Создание пользователя: {}", user.getLogin());
        return userStorage.createUser(user);
    }

    /**
     * Обновляетs существующего пользователя.
     * Выполняет валидацию пользователя и проверяет его существование перед обновлением.
     *
     * @param user Объект пользователя для обновления. Должен содержать ID существующего пользователя.
     * @return Обновленный объект пользователя.
     * @throws NotFoundException Если пользователь с указанным ID не найден (теперь делегируется userStorage).
     */
    public User updateUser(User user) {
        validateUser(user);
        // Проверка существования пользователя теперь инкапсулирована в userStorage.updateUser
        // userStorage.updateUser сам вызовет getUserByIdOrThrow(user.getId()) и выбросит NotFoundException
        // если пользователь не найден, или если update-запрос не затронул ни одной строки.
        log.info("Обновление пользователя с ID: {}", user.getId());
        return userStorage.updateUser(user);
    }

    /**
     * Возвращает список всех зарегистрированных пользователей.
     *
     * @return Список объектов User.
     */
    public List<User> getAllUsers() {
        log.info("Получение списка всех пользователей.");
        return userStorage.getAllUsers();
    }

    /**
     * Отправляет запрос в друзья от пользователя userId пользователю friendId.
     *
     * @param userId   ID пользователя, который отправляет запрос.
     * @param friendId ID пользователя, которому отправляют запрос.
     * @throws ValidationException Если userId и friendId совпадают, или запрос уже существует.
     * @throws NotFoundException   Если один из пользователей не найден.
     */
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался добавить сам себя в друзья.", userId);
            throw new ValidationException("Пользователь не может добавить сам себя в друзья.");
        }
        // Проверка существования пользователей теперь инкапсулирована в userStorage.addFriend
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь {} отправил запрос в друзья пользователю {}.", userId, friendId);
    }


    /**
     * Подтверждает заявку в друзья от friendId пользователю userId.
     *
     * @param userId   ID пользователя, который подтверждает заявку.
     * @param friendId ID пользователя, от которого пришла заявка.
     * @throws ValidationException Если userId и friendId совпадают, или ID равны null.
     * @throws NotFoundException   Если нет pending-заявки от friendId к userId.
     */
    public void acceptFriendRequest(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался принять запрос от самого себя.", userId);
            throw new ValidationException("Невозможно подтвердить запрос от самого себя.");
        }
        // userStorage.confirmFriendship теперь сам проверяет существование пользователей и наличие заявки
        userStorage.confirmFriendship(userId, friendId);
        log.info("Пользователь {} принял заявку в друзья от пользователя {}.", userId, friendId);
    }

    /**
     * Удаляет пользователя с friendId из списка друзей пользователя с userId.
     * Удаляется только односторонняя связь (userId -> friendId).
     *
     * @param userId   ID пользователя, который удаляет из друзей.
     * @param friendId ID пользователя, которого удаляют из друзей.
     * @throws ValidationException Если userId и friendId совпадают.
     * @throws NotFoundException   Если дружба (userId -> friendId) не существует.
     */
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            log.warn("Пользователь с ID {} попытался удалить сам себя из друзей.", userId);
            throw new ValidationException("Пользователь не может удалить сам себя из друзей.");
        }
        // Проверка существования пользователей и дружбы теперь инкапсулирована в userStorage.removeFriend
        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь {} удалил из друзей пользователя {}.", userId, friendId);
    }

    /**
     * Возвращает список подтвержденных друзей для userId.
     *
     * @param id ID пользователя, чьих друзей необходимо получить.
     * @return Список объектов User, представляющих друзей пользователя.
     * @throws NotFoundException Если пользователь с указанным ID не найден (делегируется userStorage).
     */
    public List<User> getFriends(Long id) {
        // Проверка существования пользователя теперь инкапсулирована в userStorage.getFriends
        log.info("Получение списка друзей для пользователя с ID: {}", id);
        return userStorage.getFriends(id);
    }

    /**
     * Возвращает список общих подтвержденных друзей между двумя пользователями.
     *
     * @param userId  ID первого пользователя.
     * @param otherId ID второго пользователя.
     * @return Список объектов User, представляющих общих друзей. Возвращает пустой список, если общих друзей нет.
     * @throws NotFoundException Если один из пользователей не найден (делегируется userStorage).
     */
    public List<User> getCommonFriends(Long userId, Long otherId) {
        // Проверка существования пользователей теперь инкапсулирована в userStorage.getCommonFriends
        log.info("Получение списка общих друзей для пользователей {} и {}.", userId, otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }

    /**
     * Возвращает пользователя по его ID.
     *
     * @param id ID пользователя.
     * @return Объект User, если пользователь найден.
     * @throws NotFoundException Если пользователь с указанным ID не найден (делегируется userStorage).
     */
    public User getUserById(Long id) {
        // userStorage.getUserById теперь сам выбрасывает NotFoundException если пользователь не найден.
        User user = userStorage.getUserById(id);
        log.info("Получение пользователя по ID: {}", id);
        return user;
    }
}


