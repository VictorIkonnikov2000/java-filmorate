
package ru.yandex.practicum.filmorate.validate;

import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

public class UserValidate {


    public static boolean validateUser(User user) {

        if (user == null) {
            throw new ValidationException("Объект пользователя не может быть null.");
        }


        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта должна быть указана, не содержать пробелов и содержать символ '@'.");
        }


        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин должен быть указан, не быть пустым и не может содержать пробелы.");
        }


        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }


        return true;
    }
}



