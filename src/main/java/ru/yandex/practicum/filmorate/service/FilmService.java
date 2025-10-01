package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Comparator; // Добавляем импорт для Comparator
import java.util.List;
import java.util.Set; // Добавляем импорт для Set
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreStorage genreStorage;
    private final MpaRatingStorage mpaRatingStorage;

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    // Вспомогательный метод для валидации и установки жанров
    private List<Genre> processFilmGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return List.of(); // Если жанров нет в запросе, возвращаем пустой список
        }

        // 1. Собираем уникальные ID жанров из пришедшего фильма, чтобы избежать дубликатов
        Set<Long> requestedGenreIds = film.getGenres().stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        // 2. Делаем ОДИН запрос к базе данных, чтобы получить все существующие жанры по этим ID
        List<Genre> existingGenres = genreStorage.getGenresByIds(requestedGenreIds);

        // 3. Проверяем, что все запрошенные ID были найдены в базе данных
        Set<Long> foundGenreIds = existingGenres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        if (foundGenreIds.size() != requestedGenreIds.size()) {
            // Находим ID жанров, которые были запрошены, но не найдены в базе
            requestedGenreIds.removeAll(foundGenreIds);
            String missingIds = requestedGenreIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            log.warn("Не найден(ы) жанр(ы) с ID: {}", missingIds);
            throw new NotFoundException("Не найден(ы) жанр(ы) с ID: " + missingIds);
        }

        // 4. Сортируем найденные жанры по их ID для обеспечения консистентного порядка
        // Это хорошая практика, так как порядок в Set и List может быть произвольным
        return existingGenres.stream()
                .sorted(Comparator.comparing(Genre::getId))
                .collect(Collectors.toList());
    }


    public Film createFilm(Film film) {
        log.debug("Начало создания фильма: {}", film);
        validateFilmReleaseDate(film);

        // Проверяем существование MPA рейтинга
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            log.warn("Не найден MPA рейтинг с ID: {}", film.getMpa().getId());
            throw new NotFoundException("MPA рейтинг с ID " + film.getMpa().getId() + " не найден.");
        }
        film.setMpa(mpa);

        // Обрабатываем жанры с улучшенной логикой
        film.setGenres(processFilmGenres(film));

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

        validateFilmReleaseDate(film);

        // Проверяем существование MPA рейтинга
        MpaRating mpa = mpaRatingStorage.getMpaById(film.getMpa().getId());
        if (mpa == null) {
            log.warn("Не найден MPA рейтинг с ID: {}", film.getMpa().getId());
            throw new NotFoundException("MPA рейтинг с ID " + film.getMpa().getId() + " не найден.");
        }
        film.setMpa(mpa);

        // Обрабатываем жанры с улучшенной логикой
        film.setGenres(processFilmGenres(film));

        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм ID {} успешно обновлен", updatedFilm.getId());
        return updatedFilm;
    }

    private void validateFilmReleaseDate(Film film) {
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".");
        }
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









