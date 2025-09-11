package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserServiceInterface {

    void addFriend(Long userId, Long friendId);

    void removeFriend(Long userId, Long friendId);

    List<User> getFriends(Long id);

    List<User> getCommonFriends(Long userId, Long otherId);
}
