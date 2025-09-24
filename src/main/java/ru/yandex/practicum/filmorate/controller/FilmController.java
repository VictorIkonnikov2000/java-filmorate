package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
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
    public ResponseEntity<Film> createFilm(@Valid @RequestBody Film film) {
        log.info("Received POST request for /films with body: {}", film);
        try {
            Film createdFilm = filmService.createFilm(film);
            log.info("Film created successfully with id: {}", createdFilm.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdFilm);  // Explicitly set status and body
        } catch (ValidationException e) { // Ловим ValidationException
            log.warn("Film validation failed: {}", e.getMessage()); // Логируем ошибку
            return ResponseEntity.badRequest().build();  // Возвращаем 400
        }
    }

    @PutMapping
    public ResponseEntity<Film> updateFilm(@Valid @RequestBody Film film) {
        log.info("Received PUT request for /films with body: {}", film);
        Film updatedFilm = filmService.updateFilm(film);
        log.info("Film updated successfully with id: {}", updatedFilm.getId());
        return ResponseEntity.ok(updatedFilm);
    }

    @GetMapping
    public ResponseEntity<List<Film>> getAllFilms() {
        log.info("Received GET request for /films");
        List<Film> films = filmService.getAllFilms();
        log.info("Returning {} films", films.size());
        return ResponseEntity.ok(films);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Film> getFilmById(@PathVariable Long id) {
        log.info("Received GET request for /films/{}", id);
        Film film = filmService.getFilmById(id);
        log.info("Returning film: {}", film);
        return ResponseEntity.ok(film);
    }

    @PutMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> addLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Received PUT request for /films/{}/like/{}", id, userId);
        filmService.addLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like/{userId}")
    public ResponseEntity<Void> removeLike(@PathVariable Long id, @PathVariable Long userId) {
        log.info("Received DELETE request for /films/{}/like/{}", id, userId);
        filmService.removeLike(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/popular")
    public ResponseEntity<List<Film>> getPopularFilms(@RequestParam(defaultValue = "10") int count) {
        log.info("Received GET request for /films/popular?count={}", count);
        List<Film> popularFilms = filmService.getPopularFilms(count);
        log.info("Returning {} popular films", popularFilms.size());
        return ResponseEntity.ok(popularFilms);
    }
}






