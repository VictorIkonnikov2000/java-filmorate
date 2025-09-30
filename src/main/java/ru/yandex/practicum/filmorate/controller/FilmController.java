package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException; // Используется для бизнес-валидации
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate; // Добавляем импорт для LocalDate
import java.util.List;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final FilmService filmService;

    // Определяем минимальную дату релиза
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public ResponseEntity<Film> createFilm(@Valid @RequestBody Film film) {
        log.info("Получен POST запрос на создание фильма: {}", film);
        // Выполняем дополнительную валидацию, которой нет во встроенных аннотациях Jakarta Validation
        validateFilm(film);

        Film createdFilm = filmService.createFilm(film);
        log.info("Фильм успешно создан с ID: {}", createdFilm.getId());
        return new ResponseEntity<>(createdFilm, HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@Valid @RequestBody Film film) {
        log.info("Получен PUT запрос на обновление фильма с телом: {}", film);
        // При обновлении также требуется валидация
        validateFilm(film);

        Film updatedFilm = filmService.updateFilm(film);
        log.info("Фильм успешно обновлен с ID: {}", updatedFilm.getId());
        return ResponseEntity.ok(updatedFilm);
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Получен GET запрос на получение всех фильмов");
        List<Film> films = filmService.getAllFilms();
        log.info("Возвращено {} фильмов", films.size());
        return ResponseEntity.ok(films);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> getFilmById(@PathVariable Long id) {
        log.info("Получен GET запрос на получение фильма по ID: {}", id);
        try {
            Film film = filmService.getFilmById(id);
            log.info("Возвращен фильм: {}", film);
            return ResponseEntity.ok(film);
        } catch (NotFoundException e) {
            log.warn("Фильм с ID {} не найден: {}", id, e.getMessage());
            // Возвращаем 404 Not Found с сообщением об ошибке в теле
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null); // Или тело с сообщением об ошибке, как ожидают тесты
            //  Map.of("error", e.getMessage())
        }
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен PUT запрос на добавление лайка фильму {} от пользователя {}", id, userId);
        filmService.addLike(id, userId);
        log.info("Лайк успешно добавлен");
        return ResponseEntity.noContent().build(); // 204 No Content - успешное выполнение без тела ответа
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен DELETE запрос на удаление лайка у фильма {} от пользователя {}", id, userId);
        filmService.removeLike(id, userId);
        log.info("Лайк успешно удален");
        return ResponseEntity.noContent().build(); // 204 No Content - успешное выполнение без тела ответа
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Film>> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        log.info("Получен GET запрос на получение {} популярных фильмов", count);
        List<Film> popularFilms = filmService.getPopularFilms(count);
        log.info("Возвращено {} популярных фильмов", popularFilms.size());
        return ResponseEntity.ok(popularFilms);
    }

    /**
     * Метод для выполнения дополнительной бизнес-валидации объекта Film.
     * Выбрасывает ValidationException, если обнаружены некорректные данные.
     *
     * @param film объект Film для валидации.
     * @throws ValidationException если данные фильма не соответствуют требованиям.
     */
    private void validateFilm(Film film) {
        // Валидация названия
        if (film.getName() == null || film.getName().isBlank()) {
            log.warn("Ошибка валидации: название фильма пустое или null. Фильм: {}", film);
            throw new ValidationException("Название фильма не может быть пустым.");
        }
        // Валидация описания
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            log.warn("Ошибка валидации: описание фильма превышает 200 символов. Длина: {}. Фильм: {}", film.getDescription().length(), film);
            throw new ValidationException("Длина описания фильма не должна превышать 200 символов.");
        }
        // Валидация даты релиза
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            log.warn("Ошибка валидации: дата релиза не может быть раньше {}. Указана: {}. Фильм: {}", MIN_RELEASE_DATE, film.getReleaseDate(), film);
            throw new ValidationException("Дата релиза не может быть раньше " + MIN_RELEASE_DATE.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")) + ".");
        }
        // Валидация продолжительности
        if (film.getDuration() != null && film.getDuration() <= 0) {
            log.warn("Ошибка валидации: продолжительность фильма должна быть положительной. Указана: {}. Фильм: {}", film.getDuration(), film);
            throw new ValidationException("Продолжительность фильма должна быть положительной.");
        }
    }
}












