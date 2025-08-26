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
    public ResponseEntity<Film> createFilm(@RequestBody Film film) {
        try {
            validateFilm(film);
            film.setId(filmIdCounter++);
            films.put(film.getId(), film); // Добавляем фильм в Map
            log.info("Добавлен фильм: {}", film);
            return new ResponseEntity<>(film, HttpStatus.CREATED);
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании фильма: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@RequestBody Film film) {
        try {
            validateFilm(film);
            if (films.containsKey(film.getId())) { // Проверяем, есть ли фильм с таким ID
                films.put(film.getId(), film); // Обновляем фильм в Map
                log.info("Обновлен фильм: {}", film);
                return new ResponseEntity<>(film, HttpStatus.OK);
            } else {
                log.warn("Фильм с id {} не найден.", film.getId());
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении фильма: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов.");
        // Получаем список всех фильмов из Map
        List<Film> filmList = films.values().stream().collect(Collectors.toList());
        return new ResponseEntity<>(filmList, HttpStatus.OK);
    }
}


