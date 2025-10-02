package ru.yandex.practicum.filmorate.validate;


import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;

public class FilmValidate {

    private static final int MAX_DESCRIPTION_LENGTH = 200;
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public static boolean validateFilm(Film film) {

        if (film.getName() == null || film.getName().isEmpty()) {
            return false;
        }


        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            return false;
        }


        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            return false;
        }


        if (film.getDuration() <= 0) {
            return false;
        }
        return true;
    }

}


