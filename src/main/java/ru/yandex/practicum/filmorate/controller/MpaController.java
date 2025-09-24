package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.MpaRatingService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j // Добавляем логирование
public class MpaController {

    private final MpaRatingService mpaRatingService;

    @GetMapping("/mpa")
    public List<MpaRating> getAllMpa() {
        log.info("Received GET request for /mpa");
        return mpaRatingService.getAllMpa();
    }

    @GetMapping("/mpa/{id}")
    public ResponseEntity<MpaRating> getMpaById(@PathVariable Long id) {
        log.info("Received GET request for /mpa/{}", id);
        try {
            MpaRating mpaRating = mpaRatingService.getMpaById(id);
            log.info("Returning MPA rating: {}", mpaRating);
            return ResponseEntity.ok(mpaRating);
        } catch (NotFoundException e) {
            log.warn("MPA rating not found with id: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Возвращаем 404
        }
    }
}
