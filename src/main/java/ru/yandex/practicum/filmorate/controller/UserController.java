package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final Map<Long, User> users = new HashMap<>();
    private Long userIdCounter = 1L;

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody User user) {
        try {
            UserValidate.validateUser(user);
            user.setId(userIdCounter++);
            users.put(user.getId(), user);
            log.info("Создан пользователь: {}", user);
            return new ResponseEntity<>(user, HttpStatus.CREATED); // Возвращаем созданного пользователя с кодом 201
        } catch (ValidationException e) {
            log.error("Ошибка валидации при создании пользователя: {}", e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // Возвращаем сообщение об ошибке и код 400
        }
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        try {
            UserValidate.validateUser(user);
            if (!users.containsKey(user.getId())) {
                log.warn("Пользователь с id {} не найден.", user.getId());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            users.put(user.getId(), user);
            log.info("Обновляем пользователя: {}", user);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (ValidationException e) {
            log.error("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Запрос на получение списка всех пользователей.");
        return new ResponseEntity<>(new ArrayList<>(users.values()), HttpStatus.OK);
    }
}



