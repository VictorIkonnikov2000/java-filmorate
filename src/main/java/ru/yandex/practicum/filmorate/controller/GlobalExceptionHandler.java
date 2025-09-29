package ru.yandex.practicum.filmorate.controller; // Или ru.yandex.practicum.filmorate.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.Map;

@RestControllerAdvice // Аннотация указывает Spring, что этот класс будет обрабатывать исключения для всех контроллеров
@Slf4j
public class GlobalExceptionHandler {

    // Обработка пользовательских ошибок валидации (например, для даты релиза, длины описания)
    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Возвращаем 400 Bad Request
    public Map<String, String> handleValidationException(ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        return Map.of("error", "Ошибка валидации", "errorMessage", e.getMessage());
    }

    // Обработка ошибок, когда ресурс не найден (например, фильм или пользователь по ID)
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND) // Возвращаем 404 Not Found
    public Map<String, String> handleNotFoundException(NotFoundException e) {
        log.warn("Ресурс не найден: {}", e.getMessage());
        return Map.of("error", "Ресурс не найден", "errorMessage", e.getMessage());
    }

    // Обработка ошибок валидации, вызванных аннотациями Jakarta Validation (@Valid)
    // Например, если в Film были @NotNull, @Size, @Min.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Возвращаем 400 Bad Request
    public Map<String, String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("Ошибка валидации входящих аргументов: {}", e.getMessage());
        // Собираем все ошибки валидации в одно сообщение
        StringBuilder errorMessage = new StringBuilder("Некорректные данные: ");
        e.getBindingResult().getFieldErrors().forEach(error ->
                errorMessage.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ")
        );
        return Map.of("error", "Ошибка валидации запроса", "errorMessage", errorMessage.toString().trim());
    }


    // Обработка всех остальных непредвиденных исключений
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Возвращаем 500 Internal Server Error
    public Map<String, String> handleGenericException(Throwable e) {
        log.error("Произошла непредвиденная ошибка на сервере: {}", e.getMessage(), e);
        return Map.of("error", "Внутренняя ошибка сервера", "errorMessage", "Произошла непредвиденная ошибка. Пожалуйста, попробуйте позже.");
    }
}