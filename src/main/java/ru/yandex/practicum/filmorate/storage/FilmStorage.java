package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {
    Film createFilm(Film film); // Возвращаем созданный фильм

    Film updateFilm(Film film); // Возвращаем обновленный фильм

    List<Film> getAllFilms();

    void addLike(Long filmId, Long userId);

    void removeLike(Long filmId, Long userId);

    List<Film> getPopularFilms(int count);

    Film getFilmById(Long filmId);
}
