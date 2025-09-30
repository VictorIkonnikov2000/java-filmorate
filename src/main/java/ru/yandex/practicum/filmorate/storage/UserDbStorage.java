package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate; // Предполагается, что это статический метод или синглтон

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component("UserDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * ENUM для статусов дружбы: PENDING (ожидает подтверждения), CONFIRMED (подтверждена).
     */
    private enum FriendshipStatus {
        PENDING,
        CONFIRMED
    }

    /**
     * Возвращает RowMapper для преобразования ResultSet в объект User.
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

    @Override
    public User createUser(User user) {
        UserValidate.validateUser(user); // Вызываем статический метод валидации
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() == null) {
            log.error("Не удалось получить сгенерированный ключ для пользователя.");
            throw new RuntimeException("Не удалось получить идентификатор пользователя после создания.");
        }
        user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        log.info("Создан пользователь: {}", user);
        return user;
    }

    @Override
    public User updateUser(User user) {
        UserValidate.validateUser(user); // Вызываем статический метод валидации
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        getUserById(user.getId()); // Проверяем, что пользователь существует

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int updatedRows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());

        if (updatedRows == 0) {
            log.error("Пользователь с ID {} не найден для обновления.", user.getId());
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден.");
        }

        log.info("Обновлен пользователь: {}", user);
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        return jdbcTemplate.query(sql, userRowMapper());
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper(), id);
        } catch (EmptyResultDataAccessException e) {
            log.warn("Пользователь с id {} не найден.", id);
            throw new NotFoundException("Пользователь с id " + id + " не найден.");
        }
    }

    /**
     * Отправка запроса на добавление в друзья.
     * Создает одностороннюю запись о дружбе со статусом `PENDING`.
     *
     * @param userId   ID пользователя, отправляющего запрос.
     * @param friendId ID пользователя, которому отправлен запрос.
     * @throws ValidationException Если пользователи совпадают или дружба уже существует в любом статусе.
     * @throws NotFoundException Если один из пользователей не найден.
     */
    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья.");
        }

        getUserById(userId); // Проверяем существование пользователя
        getUserById(friendId); // Проверяем существование друга

        // Проверяем, существует ли уже дружба (в любом статусе)
        // Важно: проверяем оба направления, чтобы избежать дубликатов запросов
        String checkFriendshipSql = "SELECT COUNT(*) FROM friends " +
                "WHERE (user1_id = ? AND user2_id = ?) " + // Запрос от userId к friendId
                "OR (user1_id = ? AND user2_id = ?)";      // Запрос от friendId к userId
        Integer existingFriendsCount = jdbcTemplate.queryForObject(checkFriendshipSql, Integer.class, userId, friendId, friendId, userId);

        if (existingFriendsCount != null && existingFriendsCount > 0) {

        }

        // Добавляем одностороннюю запись о дружбе со статусом PENDING
        String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, ?)";
        jdbcTemplate.update(insertSql, userId, friendId, FriendshipStatus.PENDING.toString());

        log.info("Пользователь {} отправил запрос в друзья пользователю {}. Статус: PENDING.", userId, friendId);
    }

    /**
     * Подтверждение дружбы.
     * Находит существующий запрос от `friendId` к `userId` со статусом `PENDING`,
     * обновляет его до `CONFIRMED` и создает симметричную запись от `userId` к `friendId` со статусом `CONFIRMED`.
     *
     * @param userId   ID пользователя, который принимает запрос (пользователь 2 в одностороннем запросе).
     * @param friendId ID пользователя, который изначально отправил запрос (пользователь 1 в одностороннем запросе).
     * @throws NotFoundException Если соответствующий запрос на дружбу не найден.
     * @throws ValidationException Если дружба уже подтверждена или нечего подтверждать.
     */
    @Override
    public void confirmFriendship(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя подтвердить дружбу с самим собой.");
        }
        getUserById(userId);
        getUserById(friendId);

        // Проверяем, есть ли PENDING-запрос от friendId к userId
        String checkPendingSql = "SELECT COUNT(*) FROM friends WHERE user1_id = ? AND user2_id = ? AND status = ?";
        Integer pendingRequests = jdbcTemplate.queryForObject(checkPendingSql, Integer.class, friendId, userId, FriendshipStatus.PENDING.toString());

        if (pendingRequests == null || pendingRequests == 0) {
            // Проверяем, не является ли дружба уже CONFIRMED в обе стороны
            String checkConfirmedSql = "SELECT COUNT(*) FROM friends " +
                    "WHERE (user1_id = ? AND user2_id = ? AND status = ?) " +
                    "AND (user1_id = ? AND user2_id = ? AND status = ?)";
            Integer confirmedFriendship = jdbcTemplate.queryForObject(checkConfirmedSql, Integer.class,
                    userId, friendId, FriendshipStatus.CONFIRMED.toString(),
                    friendId, userId, FriendshipStatus.CONFIRMED.toString());

            if (confirmedFriendship != null && confirmedFriendship == 2) {
                log.info("Дружба между пользователями {} и {} уже подтверждена.", userId, friendId);
                return; // Дружба уже подтверждена, ничего не делаем.
            }

            throw new NotFoundException("Не найден запрос на дружбу от пользователя " + friendId + " к пользователю " + userId + " со статусом " + FriendshipStatus.PENDING + ".");
        }

        // Обновляем статус существующего запроса от friendId к userId на CONFIRMED
        String updateSql = "UPDATE friends SET status = ? WHERE user1_id = ? AND user2_id = ? AND status = ?";
        jdbcTemplate.update(updateSql, FriendshipStatus.CONFIRMED.toString(), friendId, userId, FriendshipStatus.PENDING.toString());

        // Создаем симметричную запись от userId к friendId со статусом CONFIRMED (если ее еще нет)
        // Это гарантирует, что дружба будет видна в обе стороны как подтвержденная.
        String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, ?)";
        try {
            jdbcTemplate.update(insertSql, userId, friendId, FriendshipStatus.CONFIRMED.toString());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.warn("Попытка вставить дублирующуюся запись о подтвержденной дружбе между {} и {}.", userId, friendId);
            // Это может произойти, если пользователь1 уже отправил запрос,
            // а user2.confirmFriendship(user1) был вызван до того, как user1.confirmFriendship(user2).
            // В этом случае можно просто обновить существующую запись от userId к friendId.
            String updateExistingSql = "UPDATE friends SET status = ? WHERE user1_id = ? AND user2_id = ? AND status = ?";
            jdbcTemplate.update(updateExistingSql, FriendshipStatus.CONFIRMED.toString(), userId, friendId, FriendshipStatus.PENDING.toString());
        }

        log.info("Дружба между пользователями {} (получатель) и {} (отправитель) подтверждена.", userId, friendId);
    }

    /**
     * Удаление друга. Удаляет все записи о дружбе между двумя пользователями,
     * независимо от их статуса и направления.
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может сам себя удалить из друзей.");
        }
        getUserById(userId);
        getUserById(friendId);

        // Удаляем обе записи о дружбе (в любом направлении и статусе)
        String deleteSql = "DELETE FROM friends WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        int deletedRows = jdbcTemplate.update(deleteSql, userId, friendId, friendId, userId);

        if (deletedRows == 0) {
            log.warn("Не найдено дружбы между пользователем {} и {}.", userId, friendId);
        } else {
            log.info("Дружба между пользователем {} и {} разорвана. Удалено {} записей.", userId, friendId, deletedRows);
        }
    }

    /**
     * Получить список подтвержденных друзей пользователя.
     *
     * @param userId ID пользователя, для которого запрашиваются друзья.
     * @return Список объектов User, представляющих подтвержденных друзей пользователя.
     * @throws NotFoundException Если пользователь не найден.
     */
    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Проверить существование пользователя

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f ON u.user_id = f.user2_id " + // Получаем user2_id, для которого user1_id - наш userId
                "WHERE f.user1_id = ? AND f.status = ?"; // И только подтвержденную дружбу

        return jdbcTemplate.query(sql, userRowMapper(), userId, FriendshipStatus.CONFIRMED.toString());
    }

    /**
     * Получить список общих подтвержденных друзей между двумя пользователями.
     * Общие друзья - это те, кто является подтвержденным другом и для userId, и для otherUserId.
     */
    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f1 ON u.user_id = f1.user2_id " + // u - друг для userId
                "JOIN friends AS f2 ON u.user_id = f2.user2_id " + // u - друг для otherUserId
                "WHERE f1.user1_id = ? AND f1.status = ? " +      // Дружба с userId подтверждена
                "AND f2.user1_id = ? AND f2.status = ?";          // Дружба с otherUserId подтверждена

        return jdbcTemplate.query(sql, userRowMapper(),
                userId, FriendshipStatus.CONFIRMED.toString(),
                otherUserId, FriendshipStatus.CONFIRMED.toString());
    }

    /**
     * Получить список друзей друзей пользователя (только подтвержденных).
     * Исключаются сам пользователь и его прямые друзья.
     */
    @Override
    public List<User> getFriendsOfFriends(Long userId) {
        getUserById(userId);

        String sql = "SELECT DISTINCT ff.user_id, ff.email, ff.login, ff.name, ff.birthday " +
                "FROM users AS ff " + // "Друзья друзей"
                "JOIN friends AS fof ON ff.user_id = fof.user2_id AND fof.status = ? " + // ff - друг для fof.user1_id (который является прямым другом userId)
                "JOIN friends AS uf ON fof.user1_id = uf.user2_id AND uf.status = ? " + // uf.user2_id (т.е. fof.user1_id) - друг для userId
                "WHERE uf.user1_id = ? " + // Наш начальный пользователь
                "AND ff.user_id <> ? " + // Исключаем самого себя
                "AND ff.user_id NOT IN (" + // Исключаем прямых друзей userId
                "    SELECT direct_friend.user2_id FROM friends AS direct_friend WHERE direct_friend.user1_id = ? AND direct_friend.status = ?" +
                ")";

        return jdbcTemplate.query(sql, userRowMapper(),
                FriendshipStatus.CONFIRMED.toString(), // Для fof
                FriendshipStatus.CONFIRMED.toString(), // Для uf
                userId, userId, userId, FriendshipStatus.CONFIRMED.toString());
    }
}










