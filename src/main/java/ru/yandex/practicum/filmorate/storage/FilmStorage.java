package ru.yandex.practicum.filmorate.storage;

import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {
    ResponseEntity<?> createFilm(Film film);

    ResponseEntity<?> updateFilm(Film film);

    ResponseEntity<List<Film>> getAllFilms();
}
