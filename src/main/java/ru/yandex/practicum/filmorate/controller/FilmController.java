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
            validateFilm(film); // Проверяем фильм на соответствие требованиям
            film.setId(filmIdCounter++); // Увеличиваем счетчик ID и присваиваем ID фильму
            films.put(film.getId(), film); // Сохраняем фильм в хранилище
            log.info("Добавлен фильм: {}", film); // Логируем добавление фильма
            return new ResponseEntity<>(film, HttpStatus.CREATED); // Возвращаем созданный фильм и статус 201
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при создании фильма: {}", e.getMessage()); // Логируем ошибку валидации
            Map<String, String> errorResponse = new HashMap<>(); // Создаем Map для формирования JSON-ответа
            errorResponse.put("error", e.getMessage()); // Помещаем сообщение об ошибке в Map
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // Возвращаем JSON с сообщением об ошибке и статус 400
        } catch (Exception e) { // Ловим все остальные исключения
            log.error("Неожиданная ошибка при создании фильма: {}", e.getMessage(), e); // Логируем неожиданную ошибку
            Map<String, String> errorResponse = new HashMap<>(); // Создаем Map для формирования JSON-ответа
            errorResponse.put("error", "Внутренняя ошибка сервера"); // Помещаем сообщение об ошибке в Map
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // Возвращаем JSON с сообщением об ошибке и статус 500
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
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND); //Не найдено, возвращаем ответ с телом
            }
        } catch (ValidationException e) {
            log.warn("Ошибка валидации при обновлении фильма: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST); // Возвращаем JSON с ошибкой и статус 400
        } catch (Exception e) { // Ловим все остальные исключения
            log.error("Неожиданная ошибка при обновлении фильма: {}", e.getMessage(), e); // Логируем неожиданную ошибку
            Map<String, String> errorResponse = new HashMap<>(); // Создаем Map для формирования JSON-ответа
            errorResponse.put("error", "Внутренняя ошибка сервера"); // Помещаем сообщение об ошибке в Map
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // Возвращаем JSON с сообщением об ошибке и статус 500
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


