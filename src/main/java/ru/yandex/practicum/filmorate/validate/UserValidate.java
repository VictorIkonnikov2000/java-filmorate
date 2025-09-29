// UserValidate.java
package ru.yandex.practicum.filmorate.validate;

import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

public class UserValidate {

    /**
     * Валидирует объект User на соответствие бизнес-правилам.
     * При обнаружении нарушений выбрасывает ValidationException.
     * Возвращает true, если валидация успешна.
     *
     * @param user Объект User для валидации.
     * @return true, если User валиден.
     * @throws ValidationException Если User не соответствует правилам валидации.
     */
    public static boolean validateUser(User user) {
        // Проверка, что объект User не null
        if (user == null) {
            throw new ValidationException("Объект пользователя не может быть null.");
        }

        // Проверка email: не пустой, не содержит пробелов и содержит символ '@'
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта должна быть указана, не содержать пробелов и содержать символ '@'.");
        }

        // Проверка login: не пустой и не содержит пробелов
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            throw new ValidationException("Логин должен быть указан, не быть пустым и не может содержать пробелы.");
        }

        // Проверка birthday: не может быть в будущем
        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем.");
        }

        // Если все проверки пройдены, возвращаем true
        return true;
    }
}



