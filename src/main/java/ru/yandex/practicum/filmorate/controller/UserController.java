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
@Slf4j
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Обработчик исключения NotFoundException.
     * Возвращает HTTP статус 404 NOT_FOUND с сообщением об ошибке.
     * Эта аннотация позволяет Spring автоматически перехватывать NotFoundException,
     * выброшенные методами контроллера, и обрабатывать их единообразно.
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Указываем, что этот обработчик должен возвращать 404
    public Map<String, String> handleNotFoundException(NotFoundException e) {
        log.warn("Обработано исключение NotFoundException: {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }

    /**
     * Обработчик исключения ValidationException.
     * Возвращает HTTP статус 400 BAD_REQUEST с сообщением об ошибке.
     */
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationException(ValidationException e) {
        log.warn("Обработано исключение ValidationException: {}", e.getMessage());
        return Map.of("error", e.getMessage());
    }


    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        log.info("Получен запрос POST /users с телом: {}", user);
        // Обработка ValidationException теперь будет автоматически перехвачена @ExceptionHandler
        User createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        log.info("Получен запрос PUT /users с телом: {}", user);
        // NotFoundException и ValidationException будут автоматически перехвачены @ExceptionHandler
        // Общие исключения можно дополнительно отлавливать, если они не покрываются другими обработчиками
        try {
            User updatedUser = userService.updateUser(user);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (Exception e) { // Отлавливаем любые другие неожиданные исключения
            log.error("Произошла неожиданная ошибка при обновлении пользователя: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос GET /users");
        return userService.getAllUsers();
    }


    @PutMapping("/{id}/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Если дружба добавлена успешно, возвращаем 204 No Content
    public void addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос PUT /users/{}/friends/{}", id, friendId);
        // NotFoundException и ValidationException будут автоматически перехвачены @ExceptionHandler
        userService.addFriend(id, friendId);
        log.info("Пользователи {} и {} добавлены в друзья.", id, friendId);
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Если дружба удалена успешно, возвращаем 204 No Content
    public void deleteFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос DELETE /users/{}/friends/{}", id, friendId);
        // NotFoundException будет автоматически перехвачено @ExceptionHandler
        userService.removeFriend(id, friendId);
        log.info("Пользователи {} и {} удалены из друзей.", id, friendId);
    }

    @GetMapping("/{id}/friends")
    public List<User> getFriends(@PathVariable Long id) {
        log.info("Получен запрос GET /users/{}/friends", id);
        // NotFoundException будет автоматически перехвачено @ExceptionHandler
        List<User> friends = userService.getFriends(id);
        log.info("Список друзей пользователя {}: {}", id, friends);
        return friends; // Возвращаем 200 OK со списком (может быть пустым)
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        log.info("Получен запрос GET /users/{}/friends/common/{}", id, otherId);
        // NotFoundException будет автоматически перехвачено @ExceptionHandler
        List<User> commonFriends = userService.getCommonFriends(id, otherId);
        log.info("Общие друзья пользователей {} и {}: {}", id, otherId, commonFriends);
        return commonFriends; // Возвращаем 200 OK со списком (может быть пустым)
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        log.info("Запрос getUserById для id: {}", id);
        // NotFoundException будет автоматически перехвачено @ExceptionHandler
        return userService.getUserById(id); // Возвращает 200 OK
    }
}






