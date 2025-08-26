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
            return new ResponseEntity<>(user, HttpStatus.CREATED);  // Успешное создание
        } catch (ValidationException e) {
            log.error("Ошибка валидации при создании пользователя: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // ВОзвращаем 400
        }
    }

    @PutMapping
    public ResponseEntity<?> updateUser(@RequestBody User user) {
        try {
            UserValidate.validateUser(user);
            if (!users.containsKey(user.getId())) {
                log.warn("Пользователь с id {} не найден.", user.getId());
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", "Пользователь не найден");
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);  // если нет пользователя - возвращаем 404 и сообщение
            }
            users.put(user.getId(), user);
            log.info("Обновляем пользователя: {}", user);
            return new ResponseEntity<>(user, HttpStatus.OK);  // успешное обновление - возвращаем обновленный объект
        } catch (ValidationException e) {
            log.error("Ошибка валидации при обновлении пользователя: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());  // Помещаем сообщение об ошибке в Map
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);  // Возвращаем 400 и JSON
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        log.info("Запрос на получение списка всех пользователей.");
        return new ResponseEntity<>(new ArrayList<>(users.values()), HttpStatus.OK);
    }
}



