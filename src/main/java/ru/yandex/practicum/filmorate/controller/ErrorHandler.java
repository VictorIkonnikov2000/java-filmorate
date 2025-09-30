package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Убрали обработку NotFoundException, чтобы он "проваливался" в общий обработчик

    // Общий обработчик для всех остальных исключений, возвращает 500
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, String>> handleAllOtherExceptions(Throwable e) {
        log.error("Произошла непредвиденная ошибка: {}", e.getMessage(), e);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal server error: " + e.getMessage()); // Универсальное сообщение об ошибке
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


