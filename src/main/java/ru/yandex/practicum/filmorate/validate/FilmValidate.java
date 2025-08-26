package ru.yandex.practicum.filmorate.validate;

import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

public class FilmValidate {

    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public static void validateFilm(Film film) {
        // Проверяем название фильма на отсутствие или пустоту
        if (film.getName() == null || film.getName().isEmpty()) {
            throw new ValidationException("Название фильма не может быть пустым.");
        }

        // Проверяем длину описания фильма, если оно задано
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Максимальная длина описания фильма — " + MAX_DESCRIPTION_LENGTH + " символов.");
        }

        // Проверяем дату релиза фильма, если она задана, на то, что она не раньше минимальной допустимой даты
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза фильма должна быть не раньше " + MIN_RELEASE_DATE + " года.");
        }

        // Проверяем продолжительность фильма на положительное значение
        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом.");
        }
    }
}

