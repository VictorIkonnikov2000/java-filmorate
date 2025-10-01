package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException; // Импортируем наше новое исключение
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j // Добавляем логирование
@RestControllerAdvice
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

    // Обработка MethodArgumentNotValidException (HTTP 400 Bad Request) - для ошибок валидации DTO
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Ошибка валидации аргумента метода: {}", e.getMessage());

        // Извлекаем все ошибки валидации и формируем map: "поле": "сообщение об ошибке"
        Map<String, String> errors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fieldError -> fieldError.getField(), // Имя поля, в котором ошибка
                        fieldError -> fieldError.getDefaultMessage() // Сообщение об ошибке
                ));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Обработка ConstraintViolationException (HTTP 400 Bad Request) - для ошибок валидации на уровне сервиса
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("Ошибка нарушения ограничений: {}", e.getMessage());

        // Извлекаем все ошибки валидации и формируем map: "поле": "сообщение об ошибке"
        Map<String, String> errors = e.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        constraintViolation -> constraintViolation.getPropertyPath().toString(), // Путь к свойству
                        constraintViolation -> constraintViolation.getMessage() // Сообщение об ошибке
                ));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }


}

