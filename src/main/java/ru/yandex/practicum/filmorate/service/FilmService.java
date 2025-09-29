package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;

    public Film createFilm(Film film) {
        // Логируем создание фильма
        log.info("Film create: {}", film);

        // Получаем MPA рейтинг, валидируем
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            throw new NotFoundException("MpaRating not found " + film.getMpa().getId());
        }
        film.setMpa(mpa);

        // Валидируем жанры + исключаем дубликаты
        if (film.getGenres() != null) {
            List<Genre> genres = film.getGenres().stream()
                    .distinct() // <- Удаляем дубликаты тут
                    .map(genre -> genreStorage.getGenreById(genre.getId())
                            .orElseThrow(() -> new NotFoundException("Genre not found " + genre.getId())))
                    .collect(Collectors.toList());

            film.setGenres(genres);
        }

        return filmStorage.createFilm(film);
    }

    public Film updateFilm(Film film) {

        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            throw new NotFoundException("MpaRating not found " + film.getMpa().getId());
        }
        film.setMpa(mpa);

        return filmStorage.updateFilm(film);
    }

    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public void addLike(Long filmId, Long userId) {

        if (filmStorage.getFilmById(filmId) == null) {
            throw new NotFoundException("Film not found " + filmId);
        }
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("User not found " + userId);
        }
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {

        if (filmStorage.getFilmById(filmId) == null) {
            throw new NotFoundException("Film not found " + filmId);
        }
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("User not found " + userId);
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





