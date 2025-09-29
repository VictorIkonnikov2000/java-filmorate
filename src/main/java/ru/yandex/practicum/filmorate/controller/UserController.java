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

    @PostMapping
    public ResponseEntity<Object> createUser(@RequestBody User user) {
        log.info("Получен запрос POST /users с телом: {}", user);
        try {
            User createdUser = userService.createUser(user);
            return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании пользователя: {}", e.getMessage());
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping
    public ResponseEntity<Object> updateUser(@RequestBody User user) {
        log.info("Получен запрос PUT /users с телом: {}", user);
        try {
            User updatedUser = userService.updateUser(user);
            return new ResponseEntity<>(updatedUser, HttpStatus.OK);
        } catch (NotFoundException e) {
            log.warn("Пользователь не найден при обновлении с id: {}", user.getId());
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.NOT_FOUND);
        } catch (Exception e) { // Отлавливаем все остальные исключения
            log.error("Ошибка при обновлении пользователя: {}", e.getMessage(), e);
            return new ResponseEntity<>(Map.of("error", "Internal server error"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос GET /users");
        return userService.getAllUsers();//Возвращаем список всех пользователей
    }


    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<?> addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос PUT /users/{}/friends/{}", id, friendId);
        try {
            userService.addFriend(id, friendId);
            log.info("Пользователи {} и {} добавлены в друзья.", id, friendId);
            // Возвращаем 204 No Content, так как операция успешна и не возвращает тело.
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NotFoundException e) {
            log.warn("Пользователь с id {} или {} не найден.", id, friendId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); // Возвращаем 404 с телом
        } catch (ValidationException e) { // Добавляем обработку ValidationException
            log.warn("Ошибка валидации при добавлении в друзья: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<?> deleteFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос DELETE /users/{}/friends/{}", id, friendId);
        try {
            userService.removeFriend(id, friendId);
            log.info("Пользователи {} и {} удалены из друзей.", id, friendId);
            // Возвращаем 204 No Content, так как операция успешна и не возвращает тело.
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (NotFoundException e) {
            log.warn("Пользователь с id {} или {} не найден.", id, friendId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); // Возвращаем 404 с телом
        }
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<?> getFriends(@PathVariable Long id) {
        log.info("Получен запрос GET /users/{}/friends", id);
        try {
            List<User> friends = userService.getFriends(id);
            log.info("Список друзей пользователя {}: {}", id, friends);
            return new ResponseEntity<>(friends, HttpStatus.OK);
        } catch (NotFoundException e) {
            log.warn("Пользователь с id {} не найден.", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); // Возвращаем 404 с телом
        }
    }

    @GetMapping("/{id}/friends/common/{otherId}")
    public List<User> getCommonFriends(@PathVariable Long id, @PathVariable Long otherId) {
        log.info("Получен запрос GET /users/{}/friends/common/{}", id, otherId);
        List<User> commonFriends = userService.getCommonFriends(id, otherId);
        log.info("Общие друзья пользователей {} и {}: {}", id, otherId, commonFriends);
        return commonFriends;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (NotFoundException e) {
            log.warn("Пользователь с id {} не найден.", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); // Возвращаем 404 с телом
        }
    }
}





