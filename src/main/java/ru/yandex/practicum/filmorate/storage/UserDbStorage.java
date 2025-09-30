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
import ru.yandex.practicum.filmorate.validate.UserValidate;

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
        UserValidate.validateUser(user);
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            // ИЗМЕНИТЬ ЭТУ СТРОКУ:
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); // <-- Вот оно!
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        // Проверяем, что ключ был сгенерирован
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
        UserValidate.validateUser(user);
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        // Проверяем существование пользователя перед обновлением.
        // Если пользователя нет, getUserById выбросит NotFoundException,
        // который поймает ErrorHandler, вернув 404.
        // Это именно то поведение, которое ожидает тест на "User update unknown".
        getUserById(user.getId());

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());

        // Здесь можно было бы добавить логику для обработки случая,
        // если по какой-то причине rows == 0, хотя пользователь был найден.
        // Например, выбросить специфическое исключение для "обновление не удалось",
        // но в рамках типовой задачи это избыточно, так как если пользователь найден,
        // UPDATE почти всегда должен затронуть 1 строку.
        // Исключение после if(rows == 0) здесь не нужно, т.к. NotFoundException уже
        // будет пойман выше (в getUserById), если пользователя нет.

        log.info("Обновлен пользователь: {}", user);
        return user; // Возвращаем обновленного пользователя.
        // Возможно, лучше снова получить его из БД, чтобы убедиться,
        // что все поля актуальны после обновления.
        // return getUserById(user.getId()); <--- это более надежно
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

    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья.");
        }

        getUserById(userId);
        getUserById(friendId);

        String checkRelationsSql = "SELECT user1_id, user2_id, status FROM friends WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";

        List<FriendshipStatus> existingRelations = jdbcTemplate.query(checkRelationsSql, (rs, rowNum) -> {
            Long dbUser1Id = rs.getLong("user1_id");
            Long dbUser2Id = rs.getLong("user2_id");
            Boolean dbStatus = rs.getBoolean("status");
            return new FriendshipStatus(dbUser1Id, dbUser2Id, dbStatus);
        }, userId, friendId, friendId, userId /* Передаем в обоих порядках для поиска */);

        for (FriendshipStatus relation : existingRelations) {
            // Случай, когда userId (requestor) уже отправил запрос friendId (accepter)
            if (relation.requesterId.equals(userId) && relation.accepterId.equals(friendId)) {
                if (relation.isConfirmed) {
                    throw new ValidationException("Дружба между пользователем " + userId + " и " + friendId + " уже установлена.");
                } else {
                    throw new ValidationException("Запрос на дружбу от пользователя " + userId + " к " + friendId + " уже ожидает подтверждения.");
                }
            }

            if (relation.requesterId.equals(friendId) && relation.accepterId.equals(userId)) {
                if (relation.isConfirmed) {
                    throw new ValidationException("Дружба между пользователем " + friendId + " и " + userId + " уже установлена.");
                } else {
                    // Подтверждаем существующий запрос от friendId к userId
                    // Важно: confirmFriendship должен работать с (requesterId, accepterId)
                    confirmFriendship(userId, friendId); // userId подтверждает запрос от friendId
                    log.info("Замечен входящий запрос от {} к {}. Пользователь {} подтвердил этот запрос, установив взаимную дружбу.", friendId, userId, userId);
                    return; // Выходим после подтверждения
                }
            }
        }

        String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, false)"; // Используем user1_id и user2_id
        jdbcTemplate.update(insertSql, userId, friendId);
        log.info("Пользователь {} отправил запрос на дружбу пользователю {}", userId, friendId);
    }


    private static class FriendshipStatus {
        Long requesterId;
        Long accepterId;
        boolean isConfirmed;

        public FriendshipStatus(Long requesterId, Long accepterId, boolean isConfirmed) {
            this.requesterId = requesterId;
            this.accepterId = accepterId;
            this.isConfirmed = isConfirmed;
        }
    }


    @Override
    public void confirmFriendship(Long accepterId, Long requesterId) {
        getUserById(accepterId);
        getUserById(requesterId);

        String checkPendingSql = "SELECT user1_id, user2_id, status FROM friends WHERE user1_id = ? AND user2_id = ?";

        List<FriendshipStatus> existingRelations = jdbcTemplate.query(checkPendingSql, (rs, rowNum) -> {
            Long dbUser1Id = rs.getLong("user1_id");
            Long dbUser2Id = rs.getLong("user2_id");
            Boolean dbStatus = rs.getBoolean("status");
            return new FriendshipStatus(dbUser1Id, dbUser2Id, dbStatus);
        }, requesterId, accepterId); // Ищем запрос от Requester к Accepter

        if (existingRelations.isEmpty()) {
            log.warn("Ожидающий запрос от пользователя {} к пользователю {} не найден.", requesterId, accepterId);
            throw new NotFoundException("Запрос от пользователя " + requesterId + " к пользователю " + accepterId + " не найден.");
        }

        FriendshipStatus relation = existingRelations.get(0); // Должна быть только одна такая запись

        if (relation.isConfirmed) {
            log.info("Дружба между {} и {} уже подтверждена.", requesterId, accepterId);
            throw new ValidationException("Дружба уже установлена.");
        } else {
            String updateSql = "UPDATE friends SET status = true WHERE user1_id = ? AND user2_id = ?";
            jdbcTemplate.update(updateSql, requesterId, accepterId); // Обновляем именно запрос от requesterId к accepterId
            log.info("Пользователь {} подтвердил запрос от пользователя {}. Дружба установлена.", accepterId, requesterId);
        }

    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может сам себя удалить из друзей.");
        }
        getUserById(userId);
        getUserById(friendId);

        String deleteSql = "DELETE FROM friends WHERE (user1_id = ? AND user2_id = ?)";
        int deleted = jdbcTemplate.update(deleteSql, userId, friendId); // Удаляем запрос от userId к friendId

        if (deleted == 0) { // Если не удалили прямую запись, попробуем удалить обратную
            deleteSql = "DELETE FROM friends WHERE (user1_id = ? AND user2_id = ?)";
            deleted = jdbcTemplate.update(deleteSql, friendId, userId); // Удаляем запрос от friendId к userId
        }

        if (deleted == 0) {
            log.warn("Не найдено дружбы или запроса на дружбу между пользователем {} и {}.", userId, friendId);

        }
        log.info("Дружба или запрос на дружбу между пользователем {} и {} успешно удалены.", userId, friendId);
    }

    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Проверить существование пользователя

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f ON ( " +
                "    (f.user1_id = ? AND u.user_id = f.user2_id) " + // Если userId - user1_id, друг - user2_id
                "    OR " +
                "    (f.user2_id = ? AND u.user_id = f.user1_id)   " + // Если userId - user2_id, друг - user1_id
                ") " +
                "WHERE f.status = TRUE";

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId); // Передаем userId дважды
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "WHERE u.user_id IN (" +
                "    SELECT CASE WHEN f.user1_id = ? THEN f.user2_id ELSE f.user1_id END " +
                "    FROM friends AS f " +
                "    WHERE (f.user1_id = ? OR f.user2_id = ?) AND f.status = TRUE" +
                ") AND u.user_id IN (" +
                "    SELECT CASE WHEN f.user1_id = ? THEN f.user2_id ELSE f.user1_id END " +
                "    FROM friends AS f " +
                "    WHERE (f.user1_id = ? OR f.user2_id = ?) AND f.status = TRUE" +
                ")";

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId, userId, otherUserId, otherUserId, otherUserId); // Передаем параметры в правильном порядке
    }

    @Override
    public List<User> getFriendsOfFriends(Long userId) {
        getUserById(userId);

        String sql = "SELECT DISTINCT u3.user_id, u3.email, u3.login, u3.name, u3.birthday " +
                "FROM users u1 " +
                "JOIN friends f1 ON ( " +
                // u1 - это user1_id, u2 - user2_id (F1) ИЛИ u1 - user2_id, u2 - user1_id (F1)
                "    (u1.user_id = f1.user1_id AND f1.status = TRUE) OR " +
                "    (u1.user_id = f1.user2_id AND f1.status = TRUE) " +
                ") " +
                "JOIN users u2 ON ( " +
                // u2 - это друг u1 через f1
                "    (u1.user_id = f1.user1_id AND u2.user_id = f1.user2_id) OR " +
                "    (u1.user_id = f1.user2_id AND u2.user_id = f1.user1_id) " +
                ") " +
                "JOIN friends f2 ON ( " +
                // u2 - это user1_id, u3 - user2_id (F2) ИЛИ u2 - user2_id, u3 - user1_id (F2)
                "    (u2.user_id = f2.user1_id AND f2.status = TRUE) OR " +
                "    (u2.user_id = f2.user2_id AND f2.status = TRUE) " +
                ") " +
                "JOIN users u3 ON ( " +
                // u3 - это друг u2 через f2
                "    (u2.user_id = f2.user1_id AND u3.user_id = f2.user2_id) OR " +
                "    (u2.user_id = f2.user2_id AND u3.user_id = f2.user1_id) " +
                ") " +
                "WHERE u1.user_id = ? " +
                "AND u3.user_id <> ? " + // Исключаем самого себя
                "AND u3.user_id NOT IN ( " +
                "    SELECT CASE WHEN f_direct.user1_id = ? THEN f_direct.user2_id ELSE f_direct.user1_id END " +
                "    FROM friends f_direct " +
                "    WHERE (f_direct.user1_id = ? OR f_direct.user2_id = ?) AND f_direct.status = TRUE " +
                ")";

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId, userId, userId, userId);
    }
}








