package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;


import java.util.ArrayList;
import java.util.List;

import static ru.yandex.practicum.filmorate.validate.FilmValidate.validateFilm;


@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private List<Film> films = new ArrayList<>();

    private Long filmIdCounter = 1L;


    @PostMapping
    public ResponseEntity<Film> createFilm(@RequestBody Film film) {
        try {
            validateFilm(film); // Проверяем фильм на соответствие требованиям
            film.setId(filmIdCounter++); // Присваиваем ID и увеличиваем счетчик
            films.add(film); // Добавляем фильм в хранилище
            log.info("Добавлен фильм: {}", film); // Логируем добавление
            return new ResponseEntity<>(film, HttpStatus.CREATED); // Возвращаем ответ с кодом 201
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании фильма: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST); // Возвращаем 400 при ошибке валидации
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при создании фильма", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR); // Возвращаем 500 при других ошибках
        }
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@RequestBody Film film) {
        try {
            validateFilm(film); // Проверяем фильм на соответствие требованиям
            for (int i = 0; i < films.size(); i++) {
                if (films.get(i).getId().equals(film.getId())) { // Ищем фильм по ID
                    films.set(i, film); // Обновляем фильм
                    log.info("Обновлен фильм: {}", film); // Логируем обновление
                    return new ResponseEntity<>(film, HttpStatus.OK); // Возвращаем ответ с кодом 200
                }
            }
            log.warn("Фильм с id {} не найден.", film.getId());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND); // Возвращаем 404, если фильм не найден
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении фильма: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при обновлении фильма", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов.");
        return new ResponseEntity<>(films, HttpStatus.OK); // Возвращаем список фильмов с кодом 200
    }
}

