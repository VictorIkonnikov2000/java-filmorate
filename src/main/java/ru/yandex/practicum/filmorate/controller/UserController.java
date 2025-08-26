package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validate.UserValidate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    private final List<User> users = new ArrayList<>();
    private Long userIdCounter = 1L;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            UserValidate.validateUser(user);
            user.setId(userIdCounter++);
            users.add(user);
            log.info("Создан пользователь: {}", user);
            return new ResponseEntity<>(user, HttpStatus.CREATED);

        } catch (ValidationException e) { //Отлавливаем  исключение валидации
            log.error("Ошибка валидации при создании пользователя: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // возвращаем 400
        }
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        try {
            UserValidate.validateUser(user);
            List<User> updatedUsers = users.stream().map(u -> {
                if (u.getId().equals(user.getId())) {
                    log.info("Обновляем пользователя: {}", user);
                    return user;
                }
                return u;
            }).collect(Collectors.toList());

            if (users.size() == updatedUsers.size() && !users.contains(user)) { //Проверяем, был ли найден пользователь
                log.warn("Пользователь с id {} не найден.", user.getId());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND); //Если не был найден
            }
            users.clear();
            users.addAll(updatedUsers);
            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (ValidationException e) {
            log.error("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Запрос на получение списка всех пользователей.");
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}

