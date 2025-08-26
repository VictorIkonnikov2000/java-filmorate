package ru.yandex.practicum.filmorate.validate;

import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

public class UserValidate {

    public static void validateUser(User user) {
        // Проверка email на null, пустоту и наличие символа @.
        // Исправлена опечатка в названии поля email (emaill -> email)
        if (user.getEmail() == null || user.getEmail().isEmpty() || !user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта должна быть указана и содержать символ @.");
        }

        // Проверка логина на null, пустоту и наличие пробелов.
        if (user.getLogin() == null || user.getLogin().isEmpty() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин должен быть указан и не может содержать пробелы.");
        }

        // Если имя пользователя не указано, используем логин.
        if (user.getName() == null || user.getName().isEmpty()) {
            user.setName(user.getLogin()); // Устанавливаем имя пользователя равным логину
        }

        // Проверка даты рождения на null (для случаев, когда дата не указана)
        // и на то, что она не находится в будущем.
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }
    }
}


