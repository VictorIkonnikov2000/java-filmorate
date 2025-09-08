package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

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

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос PUT /films/{}/like/{}", id, userId);
        filmService.addLike(id, userId);
        log.info("Пользователь {} поставил лайк фильму {}.", userId, id);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void deleteLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Получен запрос DELETE /films/{}/like/{}", id, userId);
        filmService.removeLike(id, userId);
        log.info("Пользователь {} удалил лайк у фильма {}.", userId, id);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(@RequestParam(defaultValue = "10") Integer count) {
        log.info("Получен запрос GET /films/popular с параметром count: {}", count);
        List<Film> popularFilms = filmService.getPopularFilms(count);
        log.info("Список популярных фильмов: {}", popularFilms);
        return popularFilms;
    }
}




