package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j; // Добавляем Slf4j для логирования
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError; // Для MethodArgumentNotValidException
import org.springframework.web.bind.MethodArgumentNotValidException; // Для обработки ошибок валидации DTO
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import ru.yandex.practicum.filmorate.exception.NotFoundException; // Если у вас есть это исключение
import ru.yandex.practicum.filmorate.exception.ValidationException; // Ваше существующее исключение

import jakarta.validation.ConstraintViolationException; // Для обработки ошибок валидации полей напрямую

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors; // Для удобной обработки ошибок валидации

@Slf4j // Добавляем аннотацию Slf4j для автоматической генерации логгера
@ControllerAdvice
public class ErrorHandler {

    // Обработчик для ваших кастомных ValidationException
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, String>> handleValidationException(ValidationException e) {
        log.error("Validation error: {}", e.getMessage(), e); // Логируем ошибку с трассировкой стека
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Ошибка валидации: " + e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // Обработчик для NotFoundException (если у вас есть такое исключение, аналогично FilmDbStorage)
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(NotFoundException e) {
        log.warn("Not found error: {}", e.getMessage(), e); // Логируем ошибку как WARN, так как это не обязательно критично
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Объект не найден: " + e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Обработчик для MethodArgumentNotValidException
    // Возникает, когда @Valid используется для валидации объектов DTO в контроллере,
    // и одно из полей DTO не проходит валидацию (например, @Size, @NotNull и т.д.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("Method argument validation failed: {}", e.getMessage(), e); // Логируем ошибку
        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField, // Имя поля, где произошла ошибка
                        fieldError -> (fieldError.getDefaultMessage() != null) ? fieldError.getDefaultMessage() : "Некорректное значение" // Сообщение об ошибке
                ));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Обработчик для ConstraintViolationException
    // Возникает, когда валидация применяется на уровне методов контроллера
    // (например, с помощью @Validated на классе контроллера и аннотаций валидации на параметрах метода).
    // Или при валидации полей JPA сущностей вне контекста DTO.
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolationException(ConstraintViolationException e) {
        log.error("Constraint violation error: {}", e.getMessage(), e); // Логируем ошибку
        Map<String, String> errors = e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(), // Путь к свойству, вызвавшему ошибку
                        violation -> (violation.getMessage() != null) ? violation.getMessage() : "Нарушение ограничения" // Сообщение об ошибке
                ));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // Общий обработчик для всех остальных необработанных исключений
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Throwable e) {
        log.error("An unexpected error occurred: {}", e.getMessage(), e); // Логируем неожиданную ошибку как SEVERE
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Произошла непредвиденная ошибка: " + e.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR); // Код 500
    }
}


