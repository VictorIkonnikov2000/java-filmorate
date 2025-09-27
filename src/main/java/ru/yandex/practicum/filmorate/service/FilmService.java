package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage; // Добавлено
    private final MpaRatingStorage mpaRatingStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage, GenreStorage genreStorage, MpaRatingStorage mpaRatingStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaRatingStorage = mpaRatingStorage;
    }

    public Film createFilm(Film film) {
        log.info("Creating film: {}", film);

        // Получаем MPA рейтинг из хранилища по id
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            log.error("MpaRating with id {} not found.", film.getMpa().getId());
            throw new NotFoundException("MpaRating with id " + film.getMpa().getId() + " not found.");
        }
        film.setMpa(mpa);

        // Обрабатываем жанры
        if (film.getGenres() != null) { // Проверяем, что жанры вообще переданы
            List<Genre> genres = film.getGenres().stream()
                    .map(genre -> genreStorage.getGenreById(genre.getId())
                            .orElseThrow(() -> {
                                log.error("Genre with id {} not found.", genre.getId());
                                return new NotFoundException("Genre with id " + genre.getId() + " not found.");
                            }))
                    .collect(Collectors.toList());

            // **Удаляем дубликаты жанров**
            List<Genre> uniqueGenres = genres.stream().distinct().collect(Collectors.toList());

            film.setGenres(uniqueGenres); // Устанавливаем корректные жанры
        }

        Film createdFilm = filmStorage.createFilm(film);
        log.info("Film created successfully with id: {}", createdFilm.getId());
        return createdFilm;
    }



    public Film updateFilm(Film film) {
        // Получаем MPA рейтинг из хранилища по id
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            throw new NotFoundException("MpaRating with id " + film.getMpa().getId() + " not found.");
        }
        film.setMpa(mpa); // Устанавливаем фильм
        return filmStorage.updateFilm(film); // Возвращаем обновленный фильм
    }

    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms(); // Возвращаем список фильмов
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



