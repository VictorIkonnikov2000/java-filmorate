package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.*;


@Slf4j
public class InMemoryGenreStorage implements GenreStorage {

    private final Map<Long, Genre> genres = new HashMap<>(); // Используем Map для хранения по ID
    private Long nextId = 1L;

    public InMemoryGenreStorage() {
        // Инициализация начальными данными (пример)
        addInitialGenre(new Genre(null, "Комедия"));
        addInitialGenre(new Genre(null, "Драма"));
        addInitialGenre(new Genre(null, "Мультфильм"));
        addInitialGenre(new Genre(null, "Триллер"));
        addInitialGenre(new Genre(null, "Документальный"));
        addInitialGenre(new Genre(null, "Боевик"));
    }

    //Вспомогательный метод для инициализации
    private void addInitialGenre(Genre genre) {
        genre.setId(nextId);
        genres.put(nextId, new Genre(nextId, genre.getName()));
        nextId++;
        log.info("Добавлен новый жанр: {} с ID: {}", genre.getName(), genre.getId());
    }

    @Override
    public List<Genre> getAllGenres() {
        return new ArrayList<>(genres.values()); // Возвращаем копию, чтобы избежать изменений извне
    }

    @Override
    public Optional<Genre> getGenreById(Long id) {
        log.debug("Поиск жанра по ID: {}", id);
        Genre genre = genres.get(id);
        return Optional.ofNullable(genre); // Возвращаем Optional
    }

    @Override
    public Genre addGenre(Genre genre) {
        genre.setId(nextId);
        genres.put(nextId, new Genre(nextId, genre.getName()));
        nextId++;
        log.info("Добавлен новый жанр: {} с ID: {}", genre.getName(), genre.getId());
        return genre;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        if (!genres.containsKey(genre.getId())) {
            log.warn("Попытка обновить несуществующий жанр с ID: {}", genre.getId());
            return null; // Или выбросить исключение, если это необходимо
        }
        genres.put(genre.getId(), genre);
        log.info("Обновлен жанр с ID: {}", genre.getId());
        return genre;
    }

    @Override
    public void deleteGenre(Long id) {
        if (genres.containsKey(id)) {
            genres.remove(id);
            log.info("Удален жанр с ID: {}", id);
        } else {
            log.warn("Попытка удалить несуществующий жанр с ID: {}", id);
            // Можно выбрасывать исключение, если требуется
        }
    }
}
