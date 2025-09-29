package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataRetrievalFailureException;
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
import java.util.Optional; // Для Optional<Boolean>

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
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"user_id"}); // В SQL id обычно auto increment, поэтому указывать "user_id" как возвращаемое значение для GeneratedKeyHolder
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

        // Проверка наличия пользователя:
        // Если getUserById выбрасывает NotFoundException, то это нормально, мы его поймаем.
        // Если он возвращает null, тогда это проблема в getUserById.
        // Корректнее: getUserById сам выбрасывает исключение, нет необходимости в if (getUserById(...)).
        // Просто вызов getUserById(user.getId()) до обновления
        if (getUserById(user.getId()) == null) { // Этот вызов можно убрать, если getUserById уже бросает NotFoundException
            throw new NotFoundException("User not found for update with id: " + user.getId());
        }


        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(), Date.valueOf(user.getBirthday()), user.getId());
        if (rows == 0) { // Также проверяем, что строка была изменена
            throw new NotFoundException("User not found for update with id: " + user.getId());
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
                // Можно выбросить исключение или просто ничего не делать, в зависимости от требований
                throw new ValidationException("Friendship already accepted.");
            } else {
                log.info("Friendship request from user {} to {} already pending.", userId, friendId);
                throw new ValidationException("Friendship request already pending.");
            }
        }
        // Можно также рассмотреть случай, когда friendId уже отправил заявку userId (обратная заявка)
        // Если, например, нужно, чтобы пользователь, получающий заявку, мог сразу ее принять без создания новой
        // Это зависит от бизнес-логики. Пока оставим это вне.

        // 3. Отправляем заявку в друзья (статус false - PENDING)
        String sql = "INSERT INTO friends (user_id, friend_id, status) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, userId, friendId);
        log.info("User {} sent friend request to user {}", userId, friendId);
    }

    /**
     * Пользователь accepterId подтверждает заявку в друзья от requesterId.
     * Статус дружбы (requesterId -> accepterId) меняется на ACCEPTED (true).
     */
    public void confirmFriendship(Long accepterId, Long requesterId) {
        // Проверяем существование пользователей
        getUserById(accepterId);
        getUserById(requesterId);

        // Проверяем, существует ли pending заявка от requesterId к accepterId
        String checkSql = "SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ? AND status = false";
        Integer pendingRequests = jdbcTemplate.queryForObject(checkSql, Integer.class, requesterId, accepterId);

        if (pendingRequests == null || pendingRequests == 0) {
            throw new NotFoundException("No pending friend request from user " + requesterId + " to user " + accepterId + " found.");
        }

        // Обновляем статус заявки на true (ACCEPTED)
        String updateSql = "UPDATE friends SET status = true WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(updateSql, requesterId, accepterId);
        log.info("User {} accepted friend request from user {}", accepterId, requesterId);
    }


    /**
     * Пользователь userId удаляет friendId из своего списка друзей.
     * Удаляется запись (userId, friendId), независимо от статуса.
     * Если дружба двусторонняя, то для полного разрыва нужно удалять и (friendId, userId),
     * но по условию "односторонней" дружбы, пользователь удаляет только из СВОЕГО списка.
     */
    @Override
    public void removeFriend(Long userId, Long friendId) {
        // Проверяем существование обоих пользователей
        getUserById(userId);
        getUserById(friendId);

        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId, friendId);

        if (rowsAffected == 0) {
            // Если записей не найдено, это может быть, что такой дружбы и не было
            // или что-то пошло не так. Лучше логировать, а не выбрасывать NotFoundException,
            // т.к. "удалить то, чего нет" не всегда ошибка.
            log.warn("Attempted to remove non-existent friendship from user {} to user {}", userId, friendId);
            // Или можно выбросить NotFoundException, если это требование, чтобы удалялся
            // только существующий объект. Ваш текущий код бросает исключение, так и оставим.
            throw new NotFoundException("Friendship from user " + userId + " to user " + friendId + " not found.");
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

    // Перемещаем userRowMapper в конец или в отдельный класс для чистоты
    private RowMapper<User> userRowMapper() {
        return (rs, rowNum) -> User.builder()
                .id(rs.getLong("user_id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday") != null ? rs.getDate("birthday").toLocalDate() : null)
                .build();
    }
}



