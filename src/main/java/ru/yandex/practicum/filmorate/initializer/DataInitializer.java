package ru.yandex.practicum.filmorate.initializer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage; // Используем конкретную реализацию
import ru.yandex.practicum.filmorate.storage.MpaRatingDbStorage; // Используем конкретную реализацию
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {


    private final GenreDbStorage genreDbStorage;
    private final MpaRatingDbStorage mpaRatingDbStorage;
    private final UserDbStorage userDbStorage;


    @Override
    public void run(String... args) throws Exception {
        log.info("Запуск инициализатора данных DataInitializer...");
        // Вызываем методы инициализации из соответствующих хранилищ
        mpaRatingDbStorage.initializeMpaRatingsIfEmpty();
        genreDbStorage.initializeGenresIfEmpty();
        userDbStorage.initializeUsersIfEmpty();
        log.info("Инициализатор данных DataInitializer завершил работу.");
    }
}