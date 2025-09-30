package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.yandex.practicum.filmorate.exception.NotFoundException; // Импортируем наше новое исключение
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j // Добавляем логирование

public class ErrorHandler {

    // Обработка ValidationException (HTTP 400 Bad Request)
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage()); // Логируем предупреждение
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage()); // Сообщение об ошибке
        // Возвращаем ответ с кодом 400 (Bad Request) и JSON-ом с ошибкой
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Обработка NotFoundException (HTTP 404 Not Found)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(NotFoundException e) {
        log.warn("Ресурс не найден: {}", e.getMessage()); // Логируем предупреждение
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage()); // Сообщение о том, что ресурс не найден
        // Возвращаем ответ с кодом 404 (Not Found) и JSON-ом с ошибкой
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }


}

