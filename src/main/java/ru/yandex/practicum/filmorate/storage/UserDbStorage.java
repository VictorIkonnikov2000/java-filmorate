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
import ru.yandex.practicum.filmorate.service.UserService;
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
    private final UserService userService;


    public UserDbStorage(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    /**
     * Возвращает RowMapper для преобразования ResultSet в объект User.
     */


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

        userService.getUserById(userId); // Validate user existence.
        userService.getUserById(friendId); // Validate friend existence.

        // Check if a friendship already exists in either direction.
        String checkRelationsSql = "SELECT user1_id, user2_id, status FROM friends WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";

        List<FriendshipStatus> existingRelations = jdbcTemplate.query(checkRelationsSql, (rs, rowNum) -> {
            Long dbUser1Id = rs.getLong("user1_id");
            Long dbUser2Id = rs.getLong("user2_id");
            Boolean dbStatus = rs.getBoolean("status");
            return new FriendshipStatus(dbUser1Id, dbUser2Id, dbStatus);
        }, userId, friendId, friendId, userId); // Передаем параметры в обоих порядках

        for (FriendshipStatus relation : existingRelations) {
            if ((relation.user1Id.equals(userId) && relation.user2Id.equals(friendId)) ||
                    (relation.user1Id.equals(friendId) && relation.user2Id.equals(userId))) {
                if (relation.isConfirmed) {
                    throw new ValidationException("Дружба между пользователем " + userId + " и " + friendId + " уже установлена.");
                } else {
                    // If there's a pending request, confirm it (making it bi-directional).
                    String updateSql = "UPDATE friends SET status = true WHERE user1_id = ? AND user2_id = ?";
                    if (relation.user1Id.equals(friendId) && relation.user2Id.equals(userId)) {
                        jdbcTemplate.update(updateSql, friendId, userId); // Confirm the request from friendId to userId
                        log.info("Пользователь {} подтвердил запрос от пользователя {}. Дружба установлена.", userId, friendId);
                    } else {
                        jdbcTemplate.update(updateSql, userId, friendId);
                        log.info("Пользователь {} добавил в друзья пользователя {}. Дружба установлена.", userId, friendId);
                    }
                    String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, true)"; // Используем user1_id и user2_id
                    jdbcTemplate.update(insertSql, userId, friendId);
                    return;
                }
            }
        }
        String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, true)"; // Используем user1_id и user2_id
        jdbcTemplate.update(insertSql, userId, friendId);
        String insertSql2 = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, true)"; // Используем user1_id и user2_id
        jdbcTemplate.update(insertSql2, friendId, userId);

        log.info("Пользователь {} отправил запрос на дружбу пользователю {}", userId, friendId);
        log.info("Пользователь {} и {} стали друзьями", userId, friendId);
    }

    private static class FriendshipStatus {
        Long user1Id;
        Long user2Id;
        boolean isConfirmed;

        public FriendshipStatus(Long user1Id, Long user2Id, boolean isConfirmed) {
            this.user1Id = user1Id;
            this.user2Id = user2Id;
            this.isConfirmed = isConfirmed;
        }
    }


    @Override
    public void confirmFriendship(Long accepterId, Long requesterId) {
        userService.getUserById(accepterId);
        userService.getUserById(requesterId);

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
        userService.getUserById(userId);
        userService.getUserById(friendId);

        // Удаляем запись, где userId (это тот, кто удаляет) был user1_id, а friendId был user2_id (односторонняя дружба от userId)
        String deleteSqlUserInitiated = "DELETE FROM friends WHERE user1_id = ? AND user2_id = ?";
        int deletedUserInitiated = jdbcTemplate.update(deleteSqlUserInitiated, userId, friendId);

        // Удаляем запись, где friendId (это тот, кого удаляют) был user1_id, а userId был user2_id (односторонняя дружба от friendId)
        // Это значит, что userId должен быть удален из списка друзей friendId, если друг захочет его удалить.
        String deleteSqlFriendInitiated = "DELETE FROM friends WHERE user1_id = ? AND user2_id = ?";
        int deletedFriendInitiated = jdbcTemplate.update(deleteSqlFriendInitiated, friendId, userId);


        if (deletedUserInitiated == 0 && deletedFriendInitiated == 0) {
            log.warn("Не найдено дружбы или запроса на дружбу между пользователем {} и {}. Удаление не произведено.", userId, friendId);
            // Можно добавить NotFoundException, если ожидается, что дружба всегда существует при попытке удаления.
            // throw new NotFoundException("Дружба между пользователем " + userId + " и " + friendId + " не найдена.");
        } else {
            log.info("Дружба или запрос на дружбу между пользователем {} и {} успешно удалены ({} прямых, {} обратных).",
                    userId, friendId, deletedUserInitiated, deletedFriendInitiated);
        }
    }


    @Override
    public List<User> getFriends(Long userId) {
        userService.getUserById(userId); // Проверить существование пользователя

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f ON ( " +
                "    (f.user1_id = ? AND u.user_id = f.user2_id) " + // Если userId отправил запрос (user1_id) и u.user_id это его друг (user2_id)
                "    OR " +
                "    (f.user2_id = ? AND u.user_id = f.user1_id)   " + // Если userId получил запрос (user2_id) и u.user_id это друг, отправивший его (user1_id)
                ") " +
                "WHERE f.status = TRUE"; // Только подтвержденные друзья

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId); // Передаем userId дважды
    }

    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        userService.getUserById(userId);
        userService.getUserById(otherUserId);

        // Этот запрос корректно работает для двусторонней логики, так как он ищет
        // пользователей, которые являются друзьями как для userId, так и для otherUserId
        // с учетом статуса TRUE
        String sql = "SELECT DISTINCT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f1 ON ( " +
                "    (f1.user1_id = ? AND f1.user2_id = u.user_id) OR " +
                "    (f1.user2_id = ? AND f1.user1_id = u.user_id) " +
                ") " +
                "JOIN friends AS f2 ON ( " +
                "    (f2.user1_id = ? AND f2.user2_id = u.user_id) OR " +
                "    (f2.user2_id = ? AND f2.user1_id = u.user_id) " +
                ") " +
                "WHERE f1.status = TRUE AND f2.status = TRUE";


        return jdbcTemplate.query(sql, userRowMapper(), userId, userId, otherUserId, otherUserId);
    }

    @Override
    public List<User> getFriendsOfFriends(Long userId) {
        userService.getUserById(userId);

        // Переделаем запрос для более точной работы с двусторонней (через статусы) дружбой
        String sql = "SELECT DISTINCT u3.user_id, u3.email, u3.login, u3.name, u3.birthday " +
                "FROM friends f1 " +
                "JOIN friends f2 ON ( " +
                // Ищем друзей userId (u2) - f1
                "    (f1.user1_id = ? AND f1.status = TRUE) OR " +
                "    (f1.user2_id = ? AND f1.status = TRUE) " +
                ") " +
                "JOIN users u2 ON ( " +
                // u2 это настоящий друг userId, который либо в f1.user2_id, либо в f1.user1_id
                "    (u2.user_id = f1.user2_id AND f1.user1_id = ?) " + // u2 - друг, если userId - user1_id
                "    OR (u2.user_id = f1.user1_id AND f1.user2_id = ?) " + // u2 - друг, если userId - user2_id
                ") " +
                "JOIN users u3 ON ( " +
                // Ищем друзей u2 (u3) - f2
                "    (f2.user1_id = u2.user_id AND f2.user2_id = u3.user_id AND f2.status = TRUE) " +
                "    OR (f2.user2_id = u2.user_id AND f2.user1_id = u3.user_id AND f2.status = TRUE) " +
                ") " +
                "WHERE u3.user_id <> ? " + // Исключаем самого себя
                "AND u3.user_id NOT IN ( " +
                "    SELECT CASE WHEN f_direct.user1_id = ? THEN f_direct.user2_id ELSE f_direct.user1_id END " +
                "    FROM friends f_direct " +
                "    WHERE (f_direct.user1_id = ? OR f_direct.user2_id = ?) AND f_direct.status = TRUE " +
                ")";

        return jdbcTemplate.query(sql, userRowMapper(), userId, userId, userId, userId, userId, userId, userId, userId);
    }

    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }

    private void validateUser(User user) {
        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы.");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}








