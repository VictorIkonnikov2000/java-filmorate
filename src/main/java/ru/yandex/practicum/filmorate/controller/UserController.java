package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;

import java.util.List;

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
    public ResponseEntity<Void> addFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос PUT /users/{}/friends/{}", id, friendId);
        try {
            userService.addFriend(id, friendId);
            log.info("Пользователи {} и {} добавлены в друзья.", id, friendId);
            return ResponseEntity.status(HttpStatus.OK).build(); //Успешное добавление
        } catch (UserNotFoundException e) {
            log.warn("Пользователь с id {} или {} не найден.", id, friendId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Пользователь не найден
        }
    }

    @DeleteMapping("/{id}/friends/{friendId}")
    public ResponseEntity<Void> deleteFriend(@PathVariable Long id, @PathVariable Long friendId) {
        log.info("Получен запрос DELETE /users/{}/friends/{}", id, friendId);
        try {
            userService.removeFriend(id, friendId);
            log.info("Пользователи {} и {} удалены из друзей.", id, friendId);
            return ResponseEntity.status(HttpStatus.OK).build(); //Успешное удаление
        } catch (UserNotFoundException e) {
            log.warn("Пользователь с id {} или {} не найден.", id, friendId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Пользователь не найден
        }
    }

    @GetMapping("/{id}/friends")
    public ResponseEntity<List<User>> getFriends(@PathVariable Long id) {
        log.info("Получен запрос GET /users/{}/friends", id);
        try {
            List<User> friends = userService.getFriends(id);
            log.info("Список друзей пользователя {}: {}", id, friends);
            return new ResponseEntity<>(friends, HttpStatus.OK); //Успешное получение
        } catch (UserNotFoundException e) {
            log.warn("Пользователь с id {} не найден.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Пользователь не найден
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
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (UserNotFoundException e) {
            log.warn("Пользователь с id {} не найден.", id);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}





