package ru.yandex.practicum.filmorate.initializer; // Можно разместить в пакете `config`, `util` или `initializer`

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor // Lombok аннотация для автоматической инъекции через конструктор
public class DataInitializer implements ApplicationRunner {

    // Инжектируем GenreDbStorage. Spring гарантирует, что к этому моменту он уже создан.
    private final GenreDbStorage genreDbStorage;

    /**
     * Этот метод вызывается Spring Boot один раз после полной загрузки контекста приложения.
     * Это гарантирует, что все бины, включая DataSource и JdbcTemplate, полностью готовы к работе.
     * @param args Аргументы командной строки
     * @throws Exception Если произошла ошибка при выполнении
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Запуск инициализатора данных DataInitializer...");
        // Вызываем метод инициализации жанров, который теперь безопасен для вызова
        genreDbStorage.initializeGenresIfEmpty();
        log.info("Инициализатор данных DataInitializer завершил работу.");
    }
}
