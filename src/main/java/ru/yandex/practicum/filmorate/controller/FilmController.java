package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
        validateFilm(film);
        film.setId(filmIdCounter++);
        films.add(film);
        log.info("Добавлен фильм: {}", film);
        return new ResponseEntity<>(film, HttpStatus.CREATED);
    }


    @PutMapping
    public ResponseEntity<Film> updateFilm(@RequestBody Film film) {
        validateFilm(film);
        for (int i = 0; i < films.size(); i++) {
            if (films.get(i).getId().equals(film.getId())) {
                films.set(i, film);
                log.info("Обновлен фильм: {}", film);
                return new ResponseEntity<>(film, HttpStatus.OK);
            }
        }
        log.warn("Фильм с id {} не найден.", film.getId());
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }


    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов.");
        return new ResponseEntity<>(films, HttpStatus.OK);
    }
}

