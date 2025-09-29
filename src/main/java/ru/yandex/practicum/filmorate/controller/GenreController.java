package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.GenreService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j // Добавляем логирование
public class GenreController {

    private final GenreService genreService;

    @GetMapping("/genres")
    public List<Genre> getAllGenres() {
        log.info("Received GET request for /genres");
        return genreService.getAllGenres();
    }

    @GetMapping("/genres/{id}")
    public ResponseEntity<Genre> getGenreById(@PathVariable Integer id) { // Изменяем Long на Integer
        log.info("Received GET request for /genres/{}", id);
        // Обработка NotFoundException теперь делегируется ErrorHandler,
        // но для демонстрации можно оставить try-catch здесь.
        // Более чистое решение - использовать @ControllerAdvice (ErrorHandler в предыдущем ответе).
        try {
            Genre genre = genreService.getGenreById(Long.valueOf(id));
            log.info("Returning genre: {}", genre);
            return ResponseEntity.ok(genre);
        } catch (NotFoundException e) {
            log.warn("Genre not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Возвращаем 404
        }
    }
}


