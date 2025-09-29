package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.GenreStorage;
import ru.yandex.practicum.filmorate.storage.MpaRatingStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Map; // Добавляем импорт Map
import java.util.Set; // Добавляем импорт Set
import java.util.function.Function; // Добавляем импорт Function
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    private final @Qualifier("FilmDbStorage") FilmStorage filmStorage;
    private final @Qualifier("UserDbStorage") UserStorage userStorage;
    private final @Qualifier("GenreDbStorage") GenreStorage genreStorage;
    private final @Qualifier("MpaRatingDbStorage") MpaRatingStorage mpaRatingStorage;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);


    public Film createFilm(Film film) {
        log.debug("Начало создания фильма: {}", film);
        validateFilmReleaseDate(film);
        validateAndSetMpa(film);
        validateAndSetGenres(film); // Используем новый метод валидации жанров

        Film createdFilm = filmStorage.createFilm(film);
        log.info("Фильм ID {} успешно создан", createdFilm.getId());
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.debug("Начало обновления фильма: {}", film);
        if (filmStorage.getFilmById(film.getId()) == null) {
            log.warn("Попытка обновить несуществующий фильм с ID: {}", film.getId());
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден для обновления.");
        }

        validateFilmReleaseDate(film);
        validateAndSetMpa(film);
        validateAndSetGenres(film); // Используем новый метод валидации жанров

        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм ID {} успешно обновлен", updatedFilm.getId());
        return updatedFilm;
    }

    // Вынесенный метод для валидации даты релиза
    private void validateFilmReleaseDate(Film film) {
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".");
        }
    }

    // Вынесенный метод для валидации и установки MPA
    private void validateAndSetMpa(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("MPA рейтинг должен быть указан."); // Или другая логика, если MPA может быть null
        }
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            log.warn("Не найден MPA рейтинг с ID: {}", film.getMpa().getId());
            throw new NotFoundException("MPA рейтинг с ID " + film.getMpa().getId() + " не найден.");
        }
        film.setMpa(mpa);
    }

    /**
     * Валидирует список жанров фильма, исключает дубликаты и заменяет объекты Genre
     * актуальными данными из базы данных, выбрасывая NotFoundException, если какой-либо жанр не найден.
     * Запросы к базе данных минимизированы.
     * @param film Объект Film, для которого валидируются жанры.
     */
    private void validateAndSetGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            film.setGenres(List.of()); // Устанавливаем пустой неизменяемый список, если жанров нет
            return;
        }

        // 1. Извлекаем уникальные ID жанров из входного списка
        Set<Long> genreIdsFromRequest = film.getGenres().stream()
                .map(genre -> (long) genre.getId()) // Приводим int к Long для соответствия getGenresByIds
                .collect(Collectors.toSet()); // Set автоматически убирает дубликаты

        // 2. Запрашиваем все эти жанры одним запросом к базе данных
        List<Genre> existingGenres = genreStorage.getGenresByIds(genreIdsFromRequest);

        // 3. Преобразуем полученные жанры в Map для быстрого доступа по ID
        Map<Long, Genre> existingGenresMap = existingGenres.stream()
                .collect(Collectors.toMap(genre -> (long) genre.getId(), Function.identity()));

        // 4. Проверяем, что все запрошенные жанры действительно существуют в базе данных
        for (Long requestedGenreId : genreIdsFromRequest) {
            if (!existingGenresMap.containsKey(requestedGenreId)) {
                log.warn("Не найден жанр с ID: {}", requestedGenreId);
                throw new NotFoundException("Жанр с ID " + requestedGenreId + " не найден.");
            }
        }

        // 5. Формируем список уникальных и валидных жанров для установки в фильм
        // Используем Stream API для сохранения порядка, если он важен (хотя для жанров это редко)
        // и для использования актуальных объектов Genre из БД.
        List<Genre> distinctAndValidGenres = genreIdsFromRequest.stream()
                .map(existingGenresMap::get) // Получаем актуальный объект Genre из Map
                .collect(Collectors.toList());

        film.setGenres(distinctAndValidGenres);
    }

    public List<Film> getAllFilms() {
        log.debug("Получение всех фильмов");
        return filmStorage.getAllFilms();
    }

    public void addLike(Long filmId, Long userId) {
        log.debug("Добавление лайка фильму {} от пользователя {}", filmId, userId);
        if (filmStorage.getFilmById(filmId) == null) {
            log.warn("Попытка поставить лайк несуществующему фильму с ID: {}", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден.");
        }
        if (userStorage.getUserById(userId) == null) {
            log.warn("Попытка поставить лайк от несуществующего пользователя с ID: {}", userId);
            throw new NotFoundException("Пользователь с ID " + userId + " не найден.");
        }
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} добавил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        log.debug("Удаление лайка у фильма {} от пользователя {}", filmId, userId);
        if (filmStorage.getFilmById(filmId) == null) {
            log.warn("Попытка удалить лайк у несуществующего фильма с ID: {}", filmId);
            throw new NotFoundException("Фильм с ID " + filmId + " не найден.");
        }
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
