package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;

import java.util.List;

public interface FilmServiceInterface {
    void filmService(FilmStorage filmStorage);

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    List<Film> getPopularFilms(int count);

    Film getFilmById(Long id);

}
