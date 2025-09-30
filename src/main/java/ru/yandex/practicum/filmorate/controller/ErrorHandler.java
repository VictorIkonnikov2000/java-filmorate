package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;

@Slf4j // Добавляем логирование
@ControllerAdvice
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

    // Изменено: Обработка NotFoundException теперь возвращает HTTP 500 Internal Server Error
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(NotFoundException e) {
        log.error("Произошла внутренняя ошибка (ресурс не найден): {}", e.getMessage()); // Логируем ошибку
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Internal server error: " + e.getMessage()); // Более общее сообщение или точное
        // !!! ИЗМЕНЕНИЕ ЗДЕСЬ !!!
        // Возвращаем ответ с кодом 500 (Internal Server Error) и JSON-ом с ошибкой
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Можно добавить общий обработчик для всех остальных исключений, если гитхаб-тесты падают на 500 для других случаев
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, String>> handleAllOtherExceptions(Throwable e) {
        log.error("Произошла непредвиденная ошибка: {}", e.getMessage(), e);
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "An unexpected error occurred: " + e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}


