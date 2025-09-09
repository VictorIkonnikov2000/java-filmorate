package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.FilmNotFoundException;
import ru.yandex.practicum.filmorate.exception.UserNotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/films")
@Slf4j
public class FilmController {

    private final FilmService filmService;

    @Autowired
    public FilmController(FilmService filmService) {
        this.filmService = filmService;
    }

    @PostMapping
    public ResponseEntity<?> createFilm(@RequestBody Film film) {
        log.info("Получен запрос POST /films с телом: {}", film);
        ResponseEntity<?> response = filmService.createFilm(film);
        log.info("Ответ на запрос POST /films: {}", response);
        return response;
    }

    @PutMapping
    public ResponseEntity<?> updateFilm(@RequestBody Film film) {
        log.info("Получен запрос PUT /films с телом: {}", film);
        ResponseEntity<?> response = filmService.updateFilm(film);
        log.info("Ответ на запрос PUT /films: {}", response);
        return response;
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Получен запрос GET /films");
        ResponseEntity<List<Film>> response = filmService.getAllFilms();
        log.info("Ответ на запрос GET /films: {}", response.getBody());
        return response;
    }

    @GetMapping("/{id}") // Добавлен метод для получения фильма по ID
    public ResponseEntity<?> getFilmById(@PathVariable Long id) {
        try {
            Film film = filmService.getFilmById(id); // предполагается, что такой метод есть в FilmService
            return new ResponseEntity<>(film, HttpStatus.OK);
        } catch (FilmNotFoundException e) {
            return new ResponseEntity<>(Map.of("error", e.getMessage()), HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос PUT /films/{}/like/{}", id, userId);
        try {
            filmService.addLike(id, userId);
            log.info("Пользователь {} поставил лайк фильму {}.", userId, id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content корректнее
        } catch (FilmNotFoundException | UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // 404 если не найден
        }
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос DELETE /films/{}/like/{}", id, userId);
        try {
            filmService.removeLike(id, userId);
            log.info("Пользователь {} удалил лайк у фильма {}.", userId, id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (FilmNotFoundException | UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") Integer count) {
        log.info("Получен запрос GET /films/popular с параметром count: {}", count);
        List<Film> popularFilms = filmService.getPopularFilms(count);
        log.info("Список популярных фильмов: {}", popularFilms);
        return popularFilms;
    }

    @ExceptionHandler(FilmNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleFilmNotFoundException(FilmNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ru.yandex.practicum.filmorate.exception.UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(ru.yandex.practicum.filmorate.exception.UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }
}




