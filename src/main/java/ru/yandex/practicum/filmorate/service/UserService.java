package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.*;
import java.util.stream.Collectors;

import static ru.yandex.practicum.filmorate.validate.UserValidate.validateUser;

@Service
@RequiredArgsConstructor
public class UserService {

    private final @Qualifier("UserDbStorage") UserStorage userStorage;



    public User createUser(User user) {
        validateUser(user);  // Валидация
        return userStorage.createUser(user);
    }

    public User updateUser(User user) {
        validateUser(user); //Валидация
        return userStorage.updateUser(user); //Возвращаем обновленного пользователя
    }

    public List<User> getAllUsers() {
        return userStorage.getAllUsers(); // Возвращаем список всех пользователей
    }

    public void addFriend(Long userId, Long friendId) {
        userStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        userStorage.removeFriend(userId, friendId);
    }

    public List<User> getFriends(Long id) {
        return userStorage.getFriends(id);
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        List<User> userFriends = getFriends(userId);
        List<User> otherFriends = getFriends(otherId);

        if (userFriends == null || otherFriends == null) {
            return Collections.emptyList();
        }

        Set<Long> userFriendIds = userFriends.stream()
                .filter(Objects::nonNull) // Added null check here
                .map(User::getId)
                .collect(Collectors.toSet());

        return otherFriends.stream()
                .filter(Objects::nonNull) // Added null check here
                .filter(friend -> userFriendIds.contains(friend.getId()))
                .collect(Collectors.toList());
    }

    public User getUserById(Long id) {
        return userStorage.getUserById(id);
    }

}


