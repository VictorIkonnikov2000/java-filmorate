package ru.yandex.practicum.filmorate.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException e) {
        // Создаём мапу, чтобы сформировать JSON
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        // Возвращаем ответ с кодом 400 (Bad Request) и JSON-ом с ошибкой
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }
}

