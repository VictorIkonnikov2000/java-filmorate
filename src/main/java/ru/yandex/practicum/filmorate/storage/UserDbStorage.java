package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component("UserDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(UserDbStorage.class);

    @Autowired
    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Создает нового пользователя в базе данных.
     * Проверяет валидность пользователя перед сохранением.
     * Если имя пользователя не указано, использует логин в качестве имени.
     *
     * @param user Объект пользователя, который будет создан.
     * @return Созданный пользователь с присвоенным ID.
     * @throws ValidationException Если данные пользователя не прошли валидацию.
     */
    @Override
    public User createUser(User user) {
        if (!UserValidate.validateUser(user)) {
            log.error("Ошибка валидации пользователя при создании: {}", user);
            throw new ValidationException("User validation failed");
        }

        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();

        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"});
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        user.setId(generatedId);
        log.info("Создан пользователь: {}", user);
        return user;
    }

    /**
     * Обновляет данные существующего пользователя в базе данных.
     * Проверяет валидность пользователя перед обновлением.
     * Если имя пользователя не указано, использует логин.
     *
     * @param user Объект пользователя с обновленными данными.
     * @return Обновленный пользователь.
     * @throws ValidationException Если данные пользователя не прошли валидацию или идентификатор пользователя отсутствует.
     * @throws NotFoundException Если пользователь с указанным ID не найден.
     */
    @Override
    public User updateUser(User user) {
        if (!UserValidate.validateUser(user)) {
            log.error("Ошибка валидации пользователя при обновлении: {}", user);
            throw new ValidationException("User validation failed");
        }
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        if (user.getId() == null) {
            log.error("Идентификатор пользователя обязателен для операции обновления: {}", user);
            throw new ValidationException("User ID is required for update operation");
        }

        // Проверяем существование пользователя перед обновлением
        try {
            getUserById(user.getId());
        } catch (NotFoundException e) {
            log.error("Пользователь с id {} не найден для обновления.", user.getId());
            throw new NotFoundException("Пользователь не найден."); // Перебрасываем NotFoundException
        }


        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());
        if (rows == 0) {
            log.error("Пользователь с id {} не найден для обновления (непредвиденная ситуация после предв. проверки).", user.getId());
            throw new NotFoundException("Пользователь не найден."); // Маловероятно после предварительной проверки, но для перестраховки
        }
        log.info("Обновлен пользователь: {}", user);
        return user;
    }

    /**
     * Возвращает список всех пользователей из базы данных.
     *
     * @return Список объектов User.
     */
    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        return jdbcTemplate.query(sql, userRowMapper());
    }

    /**
     * Добавляет пользователя в друзья другому пользователю.
     * Дружба в данной реализации считается симметричной: если A добавляет B в друзья, то B также становится другом A.
     * Перед добавлением проверяет существование обоих пользователей.
     *
     * @param userId ID пользователя, добавляющего друга.
     * @param friendId ID пользователя, которого добавляют в друзья.
     * @throws NotFoundException Если один из пользователей не найден.
     */
    @Override
    public void addFriend(Long userId, Long friendId) {
        // Проверяем существование обоих пользователей
        getUserById(userId);
        getUserById(friendId);

        // Проверяем, что пользователи не пытаются добавить сами себя в друзья
        if (userId.equals(friendId)) {
            log.warn("Пользователь {} пытался добавить сам себя в друзья.", userId);
            throw new ValidationException("Пользователь не может добавить самого себя в друзья.");
        }

        // Проверяем, существует ли уже дружба userId -> friendId
        String checkFriendshipSql = "SELECT COUNT(*) FROM user_friends WHERE user_id = ? AND friend_id = ?";
        Integer existingFriendshipCount = jdbcTemplate.queryForObject(checkFriendshipSql, Integer.class, userId, friendId);

        // Добавляем дружбу, только если её ещё нет
        if (existingFriendshipCount != null && existingFriendshipCount == 0) {
            String insertFriendshipSql = "INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)";
            jdbcTemplate.update(insertFriendshipSql, userId, friendId);
            log.debug("Пользователь {} добавил пользователя {} в друзья (прямая связь).", userId, friendId);
        } else {
            log.debug("Прямая дружба между {} и {} уже существует.", userId, friendId);
        }

        // Для обеспечения симметричной дружбы также добавляем запись friendId -> userId, если её нет.
        // Хотя для получения списка друзей достаточно одной записи в одну сторону
        // (если мы ищем всех, у кого friend_id = userId ИЛИ user_id = userId),
        // чаще всего для упрощения логики удаления и поиска общих друзей хранят две записи.
        // В рамках текущих требований предполагаем хранение обеих записей.
        Integer existingReciprocalFriendshipCount = jdbcTemplate.queryForObject(checkFriendshipSql, Integer.class, friendId, userId);
        if (existingReciprocalFriendshipCount != null && existingReciprocalFriendshipCount == 0) {
            String insertReciprocalFriendshipSql = "INSERT INTO user_friends (user_id, friend_id) VALUES (?, ?)";
            jdbcTemplate.update(insertReciprocalFriendshipSql, friendId, userId);
            log.debug("Пользователь {} добавил пользователя {} в друзья (обратная связь).", friendId, userId);
        } else {
            log.debug("Обратная дружба между {} и {} уже существует.", friendId, userId);
        }
    }

    /**
     * Удаляет пользователя из друзей другого пользователя.
     * Удаляет обе записи дружбы (userId -> friendId и friendId -> userId) для поддержания симметрии.
     * Возвращает true, если хотя бы одна запись дружбы была удалена.
     *
     * @param userId ID пользователя, удаляющего друга.
     * @param friendId ID пользователя, которого удаляют из друзей.
     * @return true, если дружба была успешно удалена (или до этого не существовала в одном из направлений); false, если ни одной записи не было удалено.
     * @throws NotFoundException Если один из пользователей не найден.
     */
    @Override
    public boolean removeFriend(Long userId, Long friendId) {
        // Проверяем существование обоих пользователей
        getUserById(userId);
        getUserById(friendId);

        // Удаляем прямую дружбу (userId -> friendId)
        String deleteFriendshipSql = "DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?";
        int deletedRows1 = jdbcTemplate.update(deleteFriendshipSql, userId, friendId);

        // Удаляем обратную дружбу (friendId -> userId) для поддержания симметричности
        String deleteReciprocalFriendshipSql = "DELETE FROM user_friends WHERE user_id = ? AND friend_id = ?";
        int deletedRows2 = jdbcTemplate.update(deleteReciprocalFriendshipSql, friendId, userId);

        // Если хотя бы одна запись была удалена, считаем операцию успешной.
        // Это покрывает случаи, когда дружба до этого была только в одном направлении,
        // или если обе записи были удалены.
        if (deletedRows1 > 0 || deletedRows2 > 0) {
            log.debug("Дружба между пользователем {} и пользователем {} удалена.", userId, friendId);
            return true;
        } else {
            log.warn("Попытка удалить несуществующую дружбу между {} и {}.", userId, friendId);
            return false;
        }
    }

    /**
     * Возвращает список друзей для указанного пользователя.
     * Если у пользователя нет друзей, возвращается пустой список.
     *
     * @param userId ID пользователя, чьих друзей нужно получить.
     * @return Список объектов User, представляющих друзей.
     * @throws NotFoundException Если пользователь с указанным ID не найден.
     */
    @Override
    public List<User> getFriends(Long userId) {
        // Проверяем существование пользователя
        getUserById(userId);

        // SQL-запрос для получения друзей пользователя.
        // Исправлено: таблица `user_friends` вместо `friends`
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday FROM users u " +
                "JOIN user_friends f ON u.user_id = f.friend_id WHERE f.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper(), userId);
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
        getUserById(userId);
        getUserById(otherId);

        // SQL-запрос для получения общих друзей.
        // Ищем друзей, которые являются друзьями для обоих указанных пользователей.
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday FROM users u " +
                "JOIN user_friends uf1 ON u.user_id = uf1.friend_id AND uf1.user_id = ? " +
                "JOIN user_friends uf2 ON u.user_id = uf2.friend_id AND uf2.user_id = ?";
        return jdbcTemplate.query(sql, userRowMapper(), userId, otherId);
    }

    /**
     * Получает пользователя по его уникальному идентификатору.
     *
     * @param id ID пользователя.
     * @return Объект User, если найден.
     * @throws NotFoundException Если пользователь с указанным ID не найден.
     */
    @Override
    public User getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper(), id);

        if (users.isEmpty()) {
            log.warn("Пользователь с id {} не найден.", id);
            throw new NotFoundException("Пользователь с id " + id + " не найден.");
        }
        return users.get(0);
    }

    /**
     * Вспомогательный метод, создающий RowMapper для сопоставления строк ResultSet с объектами User.
     *
     * @return RowMapper для объекта User.
     */
    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }
}


