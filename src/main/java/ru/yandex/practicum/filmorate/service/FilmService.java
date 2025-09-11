package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;

@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
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

        if (filmStorage.getFilmById(filmId) == null) {
            throw new NotFoundException("Film with id " + filmId + " not found.");
        }
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("User with id " + userId + " not found.");
        }

        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        // Check if film and user exist
        if (filmStorage.getFilmById(filmId) == null) {
            throw new NotFoundException("Film with id " + filmId + " not found.");
        }
        if (userStorage.getUserById(userId) == null) { // Using UserStorage
            throw new NotFoundException("User with id " + userId + " not found.");
        }
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getPopularFilms(count);
    }

    public Film getFilmById(Long id) {
        return filmStorage.getFilmById(id);
    }
}


