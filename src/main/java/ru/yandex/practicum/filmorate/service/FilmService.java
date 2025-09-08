package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.List;

@Service
public class FilmService {

    private final FilmStorage filmStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage) {
        this.filmStorage = filmStorage;
    }

    public ResponseEntity<?> createFilm(Film film) {
        return filmStorage.createFilm(film);
    }

    public ResponseEntity<?> updateFilm(Film film) {
        return filmStorage.updateFilm(film);
    }

    public ResponseEntity<List<Film>> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public void addLike(Long filmId, Long userId) {

    }

    public void removeLike(Long filmId, Long userId) {

    }

    public List<Film> getPopularFilms(int count) {

        return null;
    }
}


