package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
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
    public ResponseEntity<?> createUser(@RequestBody User user) {
        log.info("Получен запрос POST /users с телом: {}", user);
        ResponseEntity<?> response = userService.createUser(user);
        log.info("Ответ на запрос POST /users: {}", response);
        return response;
    }

    @PutMapping
    public ResponseEntity<?> updateUser(@RequestBody User user) {
        log.info("Получен запрос PUT /users с телом: {}", user);
        ResponseEntity<?> response = userService.updateUser(user);
        log.info("Ответ на запрос PUT /users: {}", response);
        return response;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Получен запрос GET /users");
        ResponseEntity<List<User>> response = userService.getAllUsers();
        log.info("Ответ на запрос GET /users: {}", response.getBody());
        return response;
    }

    @PutMapping("/{id}/friends/{friendId}")
    public ResponseEntity<?> addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос PUT /users/{}/friends/{}", id, friendId);
        try {
            userService.addFriend(id, friendId);
            log.info("Пользователи {} и {} добавлены в друзья.", id, friendId);
            return ResponseEntity.status(HttpStatus.OK).build();
        } catch (NotFoundException e) {
            log.warn("Пользователь с id {} или {} не найден.", id, friendId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage())); // Возвращаем 404 с телом
        }
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<?> deleteFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос DELETE /users/{}/friends/{}", id, friendId);
        try {
            userService.removeFriend(id, friendId);
            log.info("Пользователи {} и {} удалены из друзей.", id, friendId);
            return ResponseEntity.status(HttpStatus.OK).build();
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





