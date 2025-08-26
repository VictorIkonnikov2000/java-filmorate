package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.List;

import static ru.yandex.practicum.filmorate.validate.UserValidate.validateUser;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {


    private List<User> users = new ArrayList<>();
    private Long userIdCounter = 1L;


    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            validateUser(user); // Валидация пользователя
            user.setId(userIdCounter++);
            users.add(user);
            log.info("Создан пользователь: {}", user);
            return new ResponseEntity<>(user, HttpStatus.CREATED);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании пользователя: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // Возвращаем 400 при ошибке валидации
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при создании пользователя", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Возвращаем 500 при других ошибках
        }
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        try {
            validateUser(user);
            for (int i = 0; i < users.size(); i++) {
                if (users.get(i).getId().equals(user.getId())) {
                    users.set(i, user);
                    log.info("Обновлен пользователь: {}", user);
                    return new ResponseEntity<>(user, HttpStatus.OK);
                }
            }
            log.warn("Пользователь с id {} не найден.", user.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при обновлении пользователя", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей.");
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}
