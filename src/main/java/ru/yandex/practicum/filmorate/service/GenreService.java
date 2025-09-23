package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.GenreStorage; // Предполагаем, что есть интерфейс GenreStorage

import java.util.List;

@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreStorage genreStorage; // Поле для доступа к хранилищу жанров

    public List<Genre> getAllGenres() {
        // Получает все жанры из хранилища и возвращает их.
        return genreStorage.getAllGenres();
    }

    public Genre getGenreById(Long id) {
        // Получает жанр по ID из хранилища и возвращает его.
        return genreStorage.getGenreById(id);
    }
}

