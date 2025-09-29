// ErrorHandler.java
package ru.yandex.practicum.filmorate.controller; // Предполагаемый путь для обработчика исключений,
// обычно располагается в пакете 'controller' или 'exception'

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.yandex.practicum.filmorate.exception.NotFoundException; // Класс исключения для ненайденных ресурсов
import ru.yandex.practicum.filmorate.exception.ValidationException; // Предполагаемый класс исключения для ошибок валидации

import java.util.Map;

/**
 * Глобальный обработчик исключений для REST-контроллеров.
 * Перехватывает исключения и преобразует их в соответствующие HTTP-ответы.
 */
@Slf4j
@RestControllerAdvice // Аннотация, указывающая, что этот класс является глобальным обработчиком исключений для REST-контроллеров.
public class ErrorHandler {

    /**
     * Обрабатывает исключения типа NotFoundException, возвращая HTTP-статус 404 (Not Found).
     * @param e Исключение NotFoundException.
     * @return Map с сообщением об ошибке.
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND) // Устанавливаем HTTP-статус 404
    public Map<String, String> handleNotFoundException(final NotFoundException e) {
        log.warn("Ошибка: {}", e.getMessage()); // Логируем предупреждение о ненайденном ресурсе
        return Map.of("error", "Объект не найден", "errorMessage", e.getMessage());
    }

    /**
     * Обрабатывает исключения типа ValidationException, возвращая HTTP-статус 400 (Bad Request).
     * Это полезно для ошибок валидации входных данных.
     * @param e Исключение ValidationException.
     * @return Map с сообщением об ошибке.
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Устанавливаем HTTP-статус 400
    public Map<String, String> handleValidationException(final ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage()); // Логируем предупреждение о неверных входных данных
        return Map.of("error", "Ошибка валидации", "errorMessage", e.getMessage());
    }

    /**
     * Обрабатывает все остальные необработанные исключения, возвращая HTTP-статус 500 (Internal Server Error).
     * Это общий обработчик для непредвиденных ошибок сервера.
     * @param e Исключение Throwable.
     * @return Map с сообщением об ошибке.
     */
    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // Устанавливаем HTTP-статус 500
    public Map<String, String> handleThrowable(final Throwable e) {
        log.error("Возникла необрабатываемая ошибка: {}", e.getMessage(), e); // Логируем критическую ошибку с полным стеком
        return Map.of("error", "Произошла внутренняя ошибка сервера", "errorMessage", e.getMessage());
    }
}


