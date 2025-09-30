package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException; // Добавляем импорт ValidationException
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate; // Добавляем импорт для LocalDate
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    private final  FilmStorage filmStorage;
    private final  UserStorage userStorage; // Потребуется для проверки существования пользователя
    private final  GenreStorage genreStorage; // Потребуется для проверки существования жанра
    private final  MpaRatingStorage mpaRatingStorage; // Потребуется для проверки существования MPA

    // Минимальная дата релиза, перенесена из FilmController для централизованной валидации
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);


    public Film createFilm(Film film) {
        log.debug("Начало создания фильма: {}", film);
        // Валидация даты релиза, перенесена сюда из контроллера для проверки перед сохранением.
        // Хотя основная валидация будет в контроллере, здесь можно продублировать
        // или использовать общую валидацию.
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            // В случае ошибки валидации, выбрасываем ValidationException
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".");
        }

        // Проверяем существование MPA рейтинга
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            log.warn("Не найден MPA рейтинг с ID: {}", film.getMpa().getId());
            throw new NotFoundException("MPA рейтинг с ID " + film.getMpa().getId() + " не найден.");
        }
        film.setMpa(mpa);

        // Валидируем жанры и исключаем дубликаты
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> distinctAndValidGenres = film.getGenres().stream()
                    .distinct() // <- Удаляем дубликаты тут
                    .map(genre -> genreStorage.getGenreById(genre.getId())
                            .orElseThrow(() -> new NotFoundException("Жанр с ID " + genre.getId() + " не найден.")))
                    .collect(Collectors.toList());
            film.setGenres(distinctAndValidGenres);
        } else {
            film.setGenres(List.of()); // Устанавливаем пустой список, если жанров нет, чтобы избежать NullPointerException
        }

        Film createdFilm = filmStorage.createFilm(film);
        log.info("Фильм ID {} успешно создан", createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.debug("Начало обновления фильма: {}", film);
        // Проверяем, существует ли фильм, который мы хотим обновить
        if (filmStorage.getFilmById(film.getId()) == null) {
            log.warn("Попытка обновить несуществующий фильм с ID: {}", film.getId());
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден для обновления.");
        }

        // Валидация даты релиза при обновлении
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".");
        }

        // Проверяем существование MPA рейтинга
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            log.warn("Не найден MPA рейтинг с ID: {}", film.getMpa().getId());
            throw new NotFoundException("MPA рейтинг с ID " + film.getMpa().getId() + " не найден.");
        }
        film.setMpa(mpa);

        // Валидируем жанры и исключаем дубликаты при обновлении
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Genre> distinctAndValidGenres = film.getGenres().stream()
                    .distinct()
                    .map(genre -> genreStorage.getGenreById(genre.getId())
                            .orElseThrow(() -> new NotFoundException("Жанр с ID " + genre.getId() + " не найден.")))
                    .collect(Collectors.toList());
            film.setGenres(distinctAndValidGenres);
        } else {
            film.setGenres(List.of()); // Устанавливаем пустой список, если жанров нет
        }

        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм ID {} успешно обновлен", updatedFilm.getId());
        return updatedFilm;
    }

    public List<Film> getAllFilms() {
        log.debug("Получение всех фильмов");
        return filmStorage.getAllFilms();
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка фильму {} от пользователя {}", filmId, userId);
        // Проверяем существование фильма
        if (filmStorage.getFilmById(filmId) == null) {
            log.warn("Попытка поставить лайк несуществующему фильму с ID: {}", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден.");
        }
        // Проверяем существование пользователя
        if (userStorage.getUserById(userId) == null) {
            log.warn("Попытка поставить лайк от несуществующего пользователя с ID: {}", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден.");
        }
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} добавил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка у фильма {} от пользователя {}", filmId, userId);
        // Проверяем существование фильма
        if (filmStorage.getFilmById(filmId) == null) {
            log.warn("Попытка удалить лайк у несуществующего фильма с ID: {}", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден.");
        }
        // Проверяем существование пользователя
        if (userStorage.getUserById(userId) == null) {
            log.warn("Попытка удалить лайк от несуществующего пользователя с ID: {}", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден.");
        }
        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк у фильма {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        log.debug("Получение {} популярных фильмов", count);
        return filmStorage.getPopularFilms(count);
    }

    public Film getFilmById(Long id) {
        log.debug("Получение фильма по ID: {}", id);
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            log.warn("Фильм с ID {} не найден", id);
            throw new NotFoundException("Фильм с ID " + id + " не найден.");
        }
        return film;
    }
}









