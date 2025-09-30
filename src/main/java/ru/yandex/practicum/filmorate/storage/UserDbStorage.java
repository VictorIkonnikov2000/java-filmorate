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
import java.time.LocalDate;
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
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"}); // Используем user_id как ключ
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

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

        // Проверяем существование пользователя перед обновлением
        // Если пользователя нет, getUserById выбросит NotFoundException, который контроллер должен обработать как 404
        getUserById(user.getId());

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());

        if (rows == 0) {
            // Эта часть кода будет достигнута, только если пользователь был найден, но по какой-то причине
            // запрос update не затронул ни одной строки. Это маловероятно при правильной работе,
            // но можно оставить, как защитную меру. В большинстве случаев достаточно проверки выше.
            throw new NotFoundException("User with id " + user.getId() + " not found or update failed unexpectedly.");
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

    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья.");
        }
        // 1. Проверяем существование обоих пользователей
        getUserById(userId); // Бросит NotFoundException, если userId не найден
        getUserById(friendId); // Бросит NotFoundException, если friendId не найден

        // 2. Проверяем наличие существующих отношений:
        //    A) Пользователь userId уже отправил запрос friendId
        //    B) Пользователь friendId уже отправил запрос userId (взаимный запрос)
        //    C) Дружба уже установлена (взаимно)

        String checkRelationsSql = "SELECT user_id, friend_id, status FROM friends WHERE (user_id = ? AND friend_id = ?) OR (user_id = ? AND friend_id = ?)";
        List<FriendshipStatus> existingRelations = jdbcTemplate.query(checkRelationsSql, (rs, rowNum) -> {
            return new FriendshipStatus(rs.getLong("user_id"), rs.getLong("friend_id"), rs.getBoolean("status"));
        }, userId, friendId, friendId, userId);

        for (FriendshipStatus relation : existingRelations) {
            // Если userId уже отправил запрос friendId
            if (relation.requesterId.equals(userId) && relation.accepterId.equals(friendId)) {
                if (relation.isConfirmed) {
                    throw new ValidationException("Дружба между пользователем " + userId + " и " + friendId + " уже установлена.");
                } else {
                    throw new ValidationException("Запрос на дружбу от пользователя " + userId + " к " + friendId + " уже ожидает подтверждения.");
                }
            }
            // Если friendId уже отправил запрос userId
            if (relation.requesterId.equals(friendId) && relation.accepterId.equals(userId)) {
                if (relation.isConfirmed) {
                    throw new ValidationException("Дружба между пользователем " + friendId + " и " + userId + " уже установлена.");
                } else {
                    // Это случай, когда user2 уже отправил запрос user1, и user1 пытается добавить user2.
                    // Считаем это подтверждением.
                    confirmFriendship(userId, friendId); // userId подтверждает запрос от friendId
                    log.info("Замечен входящий запрос от {} к {}. Пользователь {} подтвердил этот запрос, установив взаимную дружбу.", friendId, userId, userId);
                    return; // Выходим, так как дружба теперь подтверждена/установлена
                }
            }
        }

        // Если не было существующих запросов или дружбы по direct или reverse direction,
        // то создаем новый запрос.
        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("Пользователь {} отправил запрос на дружбу пользователю {}", userId, friendId);
    }


    /**
     * Внутренний вспомогательный класс для представления статуса дружбы.
     * Используется для более удобной обработки результатов запроса.
     */
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

        // 1. Проверяем наличие ожидающего запроса от requesterId к accepterId
        String checkPendingSql = "SELECT status FROM friends WHERE user_id = ? AND friend_id = ?";
        Boolean isPending = null;
        try {
            isPending = jdbcTemplate.queryForObject(checkPendingSql, Boolean.class, requesterId, accepterId);
        } catch (EmptyResultDataAccessException e) {
            // Нет записи о таком запросе
            log.warn("Ожидающий запрос от пользователя {} к пользователю {} не найден.", requesterId, accepterId);
            throw new NotFoundException("Запрос от пользователя " + requesterId + " к пользователю " + accepterId + " не найден.");
        }

        if (isPending != null && !isPending) { // Если запрос существует и НЕ подтвержден (false)
            // Обновляем статус существующего запроса на true
            String updateSql = "UPDATE friends SET status = true WHERE user_id = ? AND friend_id = ?";
            jdbcTemplate.update(updateSql, requesterId, accepterId);
            log.info("Пользователь {} подтвердил запрос от пользователя {}. Дружба установлена.", accepterId, requesterId);

            // Также добавляем обратную запись для взаимной дружбы, если ее нет.
            // Статус обратной записи также должен быть true.
            String checkReverseSql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkReverseSql, Integer.class, accepterId, requesterId);

            if (count == null || count == 0) {
                String insertReverseSql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, true)";
                jdbcTemplate.update(insertReverseSql, accepterId, requesterId);
                log.info("Создана обратная запись дружбы между пользователем {} и {}.", accepterId, requesterId);
            } else {
                // Если обратная запись уже есть (даже если она была false), то нужно ее обновить на true,
                // если она была неактивной.
                String updateReverseConfirmedSql = "UPDATE friends SET status = true WHERE user_id = ? AND friend_id = ?";
                jdbcTemplate.update(updateReverseConfirmedSql, accepterId, requesterId);
            }
        } else if (isPending != null && isPending) { // Если запрос уже подтвержден (true)
            log.info("Дружба между {} и {} уже подтверждена.", requesterId, accepterId);
            throw new ValidationException("Дружба уже установлена.");
        }
        // Если isPending == null, то это уже обработано в блоке catch
    }

    @Override
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может сам себя удалить из друзей.");
        }
        // 1. Проверяем существование обоих пользователей
        getUserById(userId); // Бросит NotFoundException, если userId не найден
        getUserById(friendId); // Бросит NotFoundException, если friendId не найден// 2. Удаляем прямую связь (если userId отправил запрос friendId или уже дружит)
        String deleteDirectSql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        int deletedDirect = jdbcTemplate.update(deleteDirectSql, userId, friendId);

// 3. Удаляем обратную связь (если friendId отправил запрос userId или уже дружит)
        String deleteReverseSql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        int deletedReverse = jdbcTemplate.update(deleteReverseSql, friendId, userId);

        if (deletedDirect == 0 && deletedReverse == 0) {
            log.warn("Не найдено дружбы или запроса на дружбу между пользователем {} и {}.", userId, friendId);
            throw new NotFoundException("Дружба или запрос на дружбу между пользователем " + userId + " и " + friendId + " не найден.");
        }
        log.info("Дружба или запрос на дружбу между пользователем {} и {} успешно удалены.", userId, friendId);

    }


    @Override
    public List<User> getFriends(Long userId) {
        getUserById(userId); // Проверить существование пользователя

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f ON u.user_id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status = true";

        return jdbcTemplate.query(sql, userRowMapper(), userId);
    }



    @Override
    public List<User> getCommonFriends(Long userId, Long otherUserId) {
        getUserById(userId);
        getUserById(otherUserId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday " +
                "FROM users AS u " +
                "JOIN friends AS f1 ON u.user_id = f1.friend_id " + // F1 - друг userId
                "JOIN friends AS f2 ON u.user_id = f2.friend_id " + // F2 - друг otherUserId
                "WHERE f1.user_id = ? AND f1.status = TRUE " +      // userId дружит с F1
                "AND f2.user_id = ? AND f2.status = TRUE";          // otherUserId дружит с F2


        return jdbcTemplate.query(sql, userRowMapper(), userId, otherUserId);
    }


    @Override
    public List<User> getFriendsOfFriends(Long userId) {
        getUserById(userId);
        throw new UnsupportedOperationException("Метод 'getFriendsOfFriends' еще не реализован.");
    }

    public void initializeUsersIfEmpty() {
        String checkSql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
        if (count == null || count == 0) {
            // Нет пользователей, добавляем начальные данные (пример: администратор)
            log.info("Начальная инициализация пользователей...");
            String insertSql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(insertSql, "admin@example.com", "admin", "Администратор", LocalDate.of(1990, 1, 1));
            jdbcTemplate.update(insertSql, "user@example.com", "user", "Пользователь", LocalDate.of(1995, 5, 10));
            log.info("Начальная инициализация пользователей завершена.");
        } else {
            log.info("Пользователи уже существуют, инициализация пропущена.");
        }
    }
}








