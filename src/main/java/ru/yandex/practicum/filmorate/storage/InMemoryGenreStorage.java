package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.ArrayList;

@Component
public class InMemoryGenreStorage implements GenreStorage {

    private final List<Genre> genres = new ArrayList<>(); // Хранилище жанров в памяти

    @Override
    public List<Genre> getAllGenres() {
        return genres;
    }

    @Override
    public Genre getGenreById(Long id) {
        //TODO: Реализовать поиск жанра по ID
        return null;
    }

    @Override
    public Genre addGenre(Genre genre) {
        //TODO: Реализовать добавления жанра
        return null;
    }

    @Override
    public Genre updateGenre(Genre genre) {
        //TODO: Реализовать обновления жанра
        return null;
    }

    @Override
    public void deleteGenre(Long id) {
        //TODO: Реализовать удаления жанра

    }
}

