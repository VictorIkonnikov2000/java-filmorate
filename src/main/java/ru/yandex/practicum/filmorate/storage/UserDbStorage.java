package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
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
            throw new ValidationException("Пользователь не может быть другом самому себе.");
        }
        User user = getUserById(userId); // Проверить существование
        User friend = getUserById(friendId); // Проверить существование

        // Проверяем, существует ли уже дружба (в любом направлении)
        String checkSql = "SELECT COUNT(*) FROM friends WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, userId, friendId, friendId, userId);

        if (count > 0) {
            log.warn("Дружба между пользователем {} и {} уже установлена или существует запрос.", userId, friendId);
            throw new ValidationException("Дружба уже установлена.");
        }

        // Создаем две записи для взаимной дружбы со статусом true
        String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, true)";
        jdbcTemplate.update(insertSql, userId, friendId); // user1 --> user2
        jdbcTemplate.update(insertSql, friendId, userId); // user2 --> user1 (для взаимности)

        log.info("Пользователи {} и {} стали друзьями (взаимно).", userId, friendId);
    }

    // Если addFriend сразу устанавливает дружбу, то метод confirmFriendship может быть удален,
// или его логика должна быть изменена, если он используется для чего-то другого.
// Если confirmFriendship все же нужен, то он должен просто гарантировать, что обе записи есть и статус true.
    @Override
    public void confirmFriendship(Long requesterId, Long accepterId) {
        // В этом случае, если addFriend делает взаимную дружбу, этот метод может быть избыточен
        // или использоваться для обработки других, более сложных сценариев запросов.
        // Если он ДОЛЖЕН быть, то его реализация может быть такой:
        // 1. Проверить, существует ли запись requesterId -> accepterId. Если нет, создать.
        // 2. Обновить status обеих записей до true.

        getUserById(requesterId);
        getUserById(accepterId);

        // Удаляем старые записи запросов, если они были незавершенными
        String deletePendingSql = "DELETE FROM friends WHERE (user1_id = ? AND user2_id = ? AND status = FALSE) OR (user1_id = ? AND user2_id = ? AND status = FALSE)";
        jdbcTemplate.update(deletePendingSql, requesterId, accepterId, accepterId, requesterId);

        // Вставляем две записи подтвержденной дружбы, если их еще нет
        try {
            String insertSql = "INSERT INTO friends (user1_id, user2_id, status) VALUES (?, ?, true)";
            jdbcTemplate.update(insertSql, requesterId, accepterId);
            jdbcTemplate.update(insertSql, accepterId, requesterId);
            log.info("Дружба между {} и {} подтверждена и сделана взаимной.", requesterId, accepterId);
        } catch (DuplicateKeyException e) {
            // Если записи уже были (например, в предыдущем addFriend уже вставил true)
            log.info("Дружба между {} и {} уже была подтверждена.", requesterId, accepterId);
        }
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
    public void removeFriend(Long userId, Long friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может сам себя удалить из друзей.");
        }
        getUserById(userId);
        getUserById(friendId);

        // Удаляем запись, независимо от порядка user_id и friend_id.
        // Предполагаем, что в таблице friends есть только ОДНА запись для пары userId и friendId.
        // user1_id всегда меньше user2_id или другой порядок, который вы используете для вставки.
        // В вашем случае, addFriend вставляет (userId, friendId) как (user1_id, user2_id).
        // Поэтому удалять нужно ИМЕННО эту запись.

        // Если ваша логика в addFriend ВСЕГДА вставляет (requesterId, accepterId) как (user1_id, user2_id),
        // то удалять нужно именно ее.
        String deleteSql = "DELETE FROM friends WHERE (user1_id = ? AND user2_id = ?)";
        int deleted = jdbcTemplate.update(deleteSql, userId, friendId); // Удаляем запрос от userId к friendId

        // А если дружба была инициирована friendId к userId?
        // Тогда нужно удалить запись, где user1_id = friendId и user2_id = userId
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








