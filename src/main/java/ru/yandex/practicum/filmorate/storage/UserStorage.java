package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserStorage {
    User createUser(User user); // Возвращает User

    User updateUser(User user); // Возвращает User

    List<User> getAllUsers();

    void addFriend(Long userId, Long friendId);

    void confirmFriendship(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    List<User> getFriends(Long id);

    List<User> getCommonFriends(Long userId, Long otherId);

    User getUserById(Long id);

    List<User> getFriendsOfFriends(Long userId);
}






