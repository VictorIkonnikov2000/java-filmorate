package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataRetrievalFailureException;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component("UserDbStorage")
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    public UserDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
            // Убедитесь, что "user_id" - это правильное имя столбца с автоинкрементом
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"});
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        return user;
    }

    @Override
    public User updateUser(User user) {
        UserValidate.validateUser(user);
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin());
        }

        // Проверяем существование пользователя. getUserById выбросит NotFoundException, если его нет.
        getUserById(user.getId());

        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());

        // Если getUserById не выбросил исключение, то rows должно быть 1.
        // Если по какой-то причине rows == 0 ЗДЕСЬ, это указывает на логическую ошибку,
        // возможно, в условиях WHERE или в ID, но не на "пользователь не найден".
        // В данном случае, это не должно произойти, если getUserById сработал.
        // Для демонстрации, можно оставить как есть, в реальном проекте это может быть просто log.error.
        if (rows == 0) {
            throw new NotFoundException("Update failed for user with id: " + user.getId() + ". User found, but no rows affected.");
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT user_id, email, login, name, birthday FROM users";
        return jdbcTemplate.query(sql, userRowMapper());
    }

    /**
     * Пользователь userId отправляет заявку в друзья пользователю friendId.
     * Заявка имеет статус PENDING (false).
     */
    @Override
    public void addFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Cannot add self as friend.");
        }
        // 1. Проверяем существование обоих пользователей
        getUserById(userId);
        getUserById(friendId);

        // 2. Проверяем, существует ли уже такая заявка или дружба
        String checkSql = "SELECT status FROM friends WHERE user_id = ? AND friend_id = ?";
        Optional<Boolean> existingStatus = jdbcTemplate.query(checkSql, rs -> {
            if (rs.next()) {
                return Optional.of(rs.getBoolean("status"));
            }
            return Optional.empty();
        }, userId, friendId);

        if (existingStatus.isPresent()) {
            if (existingStatus.get()) {
                log.info("Friendship between user {} and {} already accepted.", userId, friendId);
                throw new ValidationException("Friendship already accepted.");
            } else {
                log.info("Friendship request from user {} to {} already pending.", userId, friendId);
                throw new ValidationException("Friendship request already pending.");
            }
        }

        // Дополнительная проверка, чтобы избежать дублирования обратных заявок (если это не нужно)
        // Если user2 уже отправил заявку user1
        String checkReverseSql = "SELECT status FROM friends WHERE user_id = ? AND friend_id = ?";
        Optional<Boolean> existingReverseStatus = jdbcTemplate.query(checkReverseSql, rs -> {
            if (rs.next()) {
                return Optional.of(rs.getBoolean("status"));
            }
            return Optional.empty();
        }, friendId, userId);

        if (existingReverseStatus.isPresent() && !existingReverseStatus.get()) {
            // Если есть обратная, неподтвержденная заявка, то можно сразу ее подтвердить,
            // или считать новую заявку избыточной. Зависит от бизнес-логики.
            log.info("User {} sent friend request to user {}, but user {} already has a pending request to user {}. Consider confirming existing one.", userId, friendId, friendId, userId);
            throw new ValidationException("A pending friendship request already exists from friend to user. Please confirm it.");
        }

        // 3. Отправляем заявку в друзья (статус false - PENDING)
        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("User {} sent friend request to user {}", userId, friendId);
    }

    /**
     * Пользователь accepterId подтверждает заявку в друзья от requesterId.
     * Статус дружбы (requesterId -> accepterId) меняется на ACCEPTED (true).
     */
    @Override
    public void confirmFriendship(Long accepterId, Long requesterId) {
        getUserById(accepterId);
        getUserById(requesterId);

        // 1. Проверяем, существует ли ожидающая заявка от requesterId к accepterId
        String checkPendingSql = "SELECT status FROM friends WHERE user_id = ? AND friend_id = ?";
        Optional<Boolean> pendingExistence = Optional.empty();
        try {
            pendingExistence = Optional.ofNullable(jdbcTemplate.queryForObject(checkPendingSql, Boolean.class, requesterId, accepterId));
        } catch (EmptyResultDataAccessException ignored) {
        }

        if (pendingExistence.isPresent()) {
            if (pendingExistence.get()) { // status = true
                log.info("Дружба между {} и {} уже подтверждена.", requesterId, accepterId);
                // Если это уже confirmed, то дальнейшие действия могут быть избыточными или вызвать ошибку.
                // Можно просто выйти или бросить ValidationException.
                throw new ValidationException("Запрос от пользователя " + requesterId + " к пользователю " + accepterId + " уже подтвержден.");
            } else { // status = false
                // Обновляем статус заявки (requesterId -> accepterId) на true
                String updateSql = "UPDATE friends SET status = true WHERE user_id = ? AND friend_id = ?";
                jdbcTemplate.update(updateSql, requesterId, accepterId);
                log.info("Пользователь {} подтвердил запрос от пользователя {}.", accepterId, requesterId);
            }
        } else {
            // Если заявки (requesterId -> accepterId) вообще не было
            throw new NotFoundException("Не найдена ожидающая заявка от пользователя " + requesterId + " к пользователю " + accepterId + ".");
        }

        // 2. Убедимся, что существует симметричная дружба (accepterId -> requesterId) со статусом true.
        // Это гарантирует, что дружба является взаимной.
        String checkSymmetricSql = "SELECT status FROM friends WHERE user_id = ? AND friend_id = ?";
        Optional<Boolean> symmetricExistence = Optional.empty();
        try {
            symmetricExistence = Optional.ofNullable(jdbcTemplate.queryForObject(checkSymmetricSql, Boolean.class, accepterId, requesterId));
        } catch (EmptyResultDataAccessException ignored) {
        }

        if (symmetricExistence.isEmpty() || !symmetricExistence.get()) {
            // Если симметричной записи нет или она не true, создаем/обновляем её.
            String insertOrUpdateSymmetricSql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, true) " +
                    "ON CONFLICT (user_id, friend_id) DO UPDATE SET status = true";
            // ON CONFLICT - это PostgreSQL специфично. Для H2/MySQL может потребоваться другая логика
            // (сначала SELECT, потом INSERT или UPDATE).
            // Для H2: MERGE INTO friends (user_id, friend_id, status) KEY(user_id, friend_id) VALUES (?, ?, true)
            jdbcTemplate.update(insertOrUpdateSymmetricSql, accepterId, requesterId);
            log.info("Создана/обновлена симметричная запись дружбы между {} и {} со статусом true.", accepterId, requesterId);
        }
    }


    /**
     * Пользователь userId удаляет friendId из своего списка друзей.
     * Удаляется запись (userId, friendId), независимо от статуса.
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        // Проверяем существование обоих пользователей
        getUserById(userId);
        getUserById(friendId);

        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId, friendId);

        if (rowsAffected == 0) {
            // Если записей не найдено, это означает, что дружбы не было.
            // Ваш текущий код бросает NotFoundException, и это нормально, если
            // API должен возвращать 404 при попытке удалить несуществующий ресурс/связь.
            log.warn("Attempted to remove non-existent friendship from user {} to user {}", userId, friendId);
            throw new NotFoundException("Friendship from user " + userId + " to user " + friendId + " not found to remove.");
        }
        log.info("User {} removed friend {}", userId, friendId);
    }

    /**
     * Возвращает список подтвержденных друзей для userId.
     */
    @Override
    public List<User> getFriends(Long userId) {
        // Проверяем существование пользователя
        getUserById(userId);

        // Запрос выбирает друзей, которых userId добавил И которые подтверждены (status = true)
        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday FROM users u " +
                "JOIN friends f ON u.user_id = f.friend_id WHERE f.user_id = ? AND f.status = true";
        return jdbcTemplate.query(sql, userRowMapper(), userId);
    }

    /**
     * Возвращает список общих подтвержденных друзей для userId и otherId.
     * Общие друзья - это пользователи, которые являются подтвержденными друзьями
     * как для userId, так и для otherId.
     */
    @Override
    public List<User> getCommonFriends(Long userId, Long otherId) {
        // Проверяем существование пользователей
        getUserById(userId);
        getUserById(otherId);

        String sql = "SELECT u.user_id, u.email, u.login, u.name, u.birthday FROM users u " +
                "WHERE u.user_id IN (SELECT friend_id FROM friends WHERE user_id = ? AND status = true) " +
                "AND u.user_id IN (SELECT friend_id FROM friends WHERE user_id = ? AND status = true)";
        return jdbcTemplate.query(sql, userRowMapper(), userId, otherId);
    }

    @Override
    public User getUserById(Long id) {
        String sql = "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, userRowMapper(), id);
        } catch (DataRetrievalFailureException e) {
            log.warn("User with id {} not found or multiple users found for ID. Details: {}", id, e.getMessage());
            throw new NotFoundException("User not found with id: " + id);
        }
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


}








