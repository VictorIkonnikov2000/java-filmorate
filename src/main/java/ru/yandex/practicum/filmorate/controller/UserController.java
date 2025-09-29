package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Slf4j // Использование Lombok для логирования
public class UserController {

    private final UserService userService;

    // Внедрение зависимости UserService через конструктор
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Обрабатывает POST-запросы для создания нового пользователя.
     *
     * @param user Объект User из тела запроса.
     * @return ResponseEntity с созданным пользователем и статусом 201 (CREATED),
     *         или с сообщением об ошибке и статусом 400 (BAD_REQUEST) в случае валидационной ошибки.
     */
    @PostMapping
    public ResponseEntity<Object> createUser(@RequestBody User user) {
        log.info("Получен запрос POST /users с телом: {}", user);
        try {
            User createdUser = userService.createUser(user);
            log.info("Пользователь успешно создан: {}", createdUser);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании пользователя: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Обрабатывает PUT-запросы для обновления существующего пользователя.
     *
     * @param user Объект User из тела запроса (должен содержать ID).
     * @return ResponseEntity с обновленным пользователем и статусом 200 (OK),
     *         сообщением об ошибке и статусом 400 (BAD_REQUEST) при валидации,
     *         или статусом 404 (NOT_FOUND) если пользователь не найден.
     */
    @PutMapping
    public ResponseEntity<Object> updateUser(@RequestBody User user) {
        log.info("Получен запрос PUT /users с телом: {}", user);
        try {
            User updatedUser = userService.updateUser(user);
            log.info("Пользователь успешно обновлен: {}", updatedUser);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
        } catch (NotFoundException e) {
            log.warn("Пользователь не найден при обновлении с id: {}", user.getId());
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) { // Отлавливаем все остальные непредвиденные исключения
            log.error("Произошла непредвиденная ошибка при обновлении пользователя {}: {}", user.getId(), e.getMessage(), e);
            return new ResponseEntity<>(Map.of("error", "Произошла внутренняя ошибка сервера."), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Обрабатывает GET-запросы для получения списка всех пользователей.
     *
     * @return Список всех пользователей.
     */
    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос GET /users");
        List<User> users = userService.getAllUsers();
        log.info("Возвращен список всех пользователей, количество: {}", users.size());
        return users;
    }

    /**
     * Обрабатывает PUT-запросы для добавления пользователя в друзья.
     *
     * @param id       ID пользователя, который добавляет друга.
     * @param friendId ID пользователя, которого добавляют в друзья.
     * @return ResponseEntity со статусом 200 (OK) при успешном добавлении,
     *         или 404 (NOT_FOUND) если один из пользователей не найден.
     */
    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<?> addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос PUT /users/{}/friends/{}", id, friendId);
        try {
            userService.addFriend(id, friendId); // Вызываем метод сервиса для добавления друга
            log.info("Пользователи {} и {} добавлены в друзья.", id, friendId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (NotFoundException e) {
            log.warn("Ошибка добавления в друзья: пользователь с id {} или {} не найден. {}", id, friendId, e.getMessage());
            // Возвращаем 404 Not Found с телом, содержащим сообщение об ошибке
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при добавлении в друзья: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка при добавлении в друзья {} и {}: {}", id, friendId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Произошла внутренняя ошибка сервера."));
        }
    }

    /**
     * Обрабатывает DELETE-запросы для удаления пользователя из друзей.
     *
     * @param id       ID пользователя, который удаляет друга.
     * @param friendId ID пользователя, которого удаляют из друзей.
     * @return ResponseEntity со статусом 200 (OK) при успешном удалении,
     *         или 404 (NOT_FOUND) если дружба не найдена.
     */
    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<?> deleteFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос DELETE /users/{}/friends/{}", id, friendId);
        try {
            userService.removeFriend(id, friendId); // Вызываем метод сервиса для удаления друга
            log.info("Пользователи {} и {} удалены из друзей.", id, friendId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (NotFoundException e) {
            log.warn("Ошибка при удалении дружбы: для пользователей {} и {} не найдена. {}", id, friendId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка при удалении из друзей {} и {}: {}", id, friendId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Произошла внутренняя ошибка сервера."));
        }
    }

    /**
     * Обрабатывает GET-запросы для получения списка друзей пользователя.
     *
     * @param id ID пользователя.
     * @return ResponseEntity со списком друзей и статусом 200 (OK),
     *         или 404 (NOT_FOUND) если пользователь не найден.
     */
    @GetMapping("/{id}/friends")
    public ResponseEntity<?> getFriends(@PathVariable Long id) {
        log.info("Получен запрос GET /users/{}/friends", id);
        try {
            List<User> friends = userService.getFriends(id); // Вызываем метод сервиса для получения друзей
            log.info("Список друзей пользователя {}: {}", id, friends);
            return new ResponseEntity<>(friends, HttpStatus.OK);
        } catch (NotFoundException e) {
            log.warn("Пользователь с id {} не найден при запросе друзей. {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка при получении друзей пользователя {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Произошла внутренняя ошибка сервера."));
        }
    }

    /**
     * Обрабатывает GET-запросы для получения списка общих друзей двух пользователей.
     *
     * @param id      ID первого пользователя.
     * @param otherId ID второго пользователя.
     * @return Список общих друзей.
     */
    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        log.info("Получен запрос GET /users/{}/friends/common/{}", id, otherId);
        List<User> commonFriends = userService.getCommonFriends(id, otherId); // Вызываем метод сервиса
        log.info("Общие друзья пользователей {} и {}: {}", id, otherId, commonFriends);
        return commonFriends;
    }

    /**
     * Обрабатывает GET-запросы для получения пользователя по его ID.
     *
     * @param id ID пользователя.
     * @return ResponseEntity с найденным пользователем и статусом 200 (OK),
     *         или 404 (NOT_FOUND) если пользователь не найден.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        log.info("Получен запрос GET /users/{}", id);
        try {
            User user = userService.getUserById(id); // Вызываем метод сервиса для получения пользователя
            log.info("Пользователь с id {} найден: {}", id, user);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (NotFoundException e) {
            log.warn("Пользователь с id {} не найден. {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Произошла непредвиденная ошибка при получении пользователя {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Произошла внутренняя ошибка сервера."));
        }
    }
}






