package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
    public ResponseEntity<User> createUser(@RequestBody User user) {
        log.info("Получен запрос POST /users с телом: {}", user);
        // Обработка ValidationException теперь будет автоматически перехвачена @ExceptionHandler
        User createdUser = userService.createUser(user);
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<User> updateUser(@RequestBody User user) {
        log.info("Получен запрос PUT /users с телом: {}", user);
        // NotFoundException и ValidationException теперь будут автоматически перехвачены @ExceptionHandler
        User updatedUser = userService.updateUser(user);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
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






