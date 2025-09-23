package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class InMemoryGenreStorage implements GenreStorage {

    private final Map<Long, Genre> genres = new HashMap<>(); // Используем Map для хранения по ID
    private Long nextId = 1L;

    public InMemoryGenreStorage() {
        // Инициализация начальными данными (пример)
        addGenre(new Genre(null, "Комедия"));
        addGenre(new Genre(null, "Драма"));
        addGenre(new Genre(null, "Мультфильм"));
        addGenre(new Genre(null, "Триллер"));
        addGenre(new Genre(null, "Документальный"));
        addGenre(new Genre(null, "Боевик"));
    }

    @Override
    public List<Genre> getAllGenres() {
        return new ArrayList<>(genres.values()); // Возвращаем копию, чтобы избежать изменений извне
    }

    @Override
    public Genre getGenreById(Long id) {
        log.debug("Поиск жанра по ID: {}", id);
        return genres.get(id);
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

