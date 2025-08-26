package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static ru.yandex.practicum.filmorate.validate.FilmValidate.validateFilm;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final Map<Long, Film> films = new HashMap<>(); // Используем Map для хранения фильмов
    private Long filmIdCounter = 1L;

    @PostMapping
    public ResponseEntity<?> createFilm(@RequestBody Film film) {
        try {
            validateFilm(film);
            film.setId(filmIdCounter++);
            films.put(film.getId(), film);
            log.info("Добавлен фильм: {}", film);
            return new ResponseEntity<>(film, HttpStatus.CREATED);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании фильма: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при создании фильма: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Внутренняя ошибка сервера");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping
    public ResponseEntity<?> updateFilm(@RequestBody Film film) {
        try {
            validateFilm(film);
            if (films.containsKey(film.getId())) {
                films.put(film.getId(), film);
                log.info("Обновлен фильм: {}", film);
                return new ResponseEntity<>(film, HttpStatus.OK);
            } else {
                String errorMessage = "Фильм с id " + film.getId() + " не найден.";
                log.warn(errorMessage);
                Map<String, String> errorResponse = new HashMap<>();
                errorResponse.put("error", errorMessage);
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении фильма: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при обновлении фильма: {}", e.getMessage(), e);
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Внутренняя ошибка сервера");
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов.");
        List<Film> filmList = films.values().stream().collect(Collectors.toList());
        return new ResponseEntity<>(filmList, HttpStatus.OK);
    }
}


