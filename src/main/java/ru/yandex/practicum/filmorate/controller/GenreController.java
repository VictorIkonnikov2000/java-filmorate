package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.service.GenreService;


import java.util.List;

@RestController
@RequiredArgsConstructor // Создает конструктор с @Autowired для полей, помеченных final
public class GenreController {

    private final GenreService genreService; // Сервис для работы с жанрами

    @GetMapping("/genres")
    public List<Genre> getAllGenres() { // Получаем список всех жанров
        return genreService.getAllGenres(); // Вызываем метод сервиса для получения списка
    }

    @GetMapping("/genres/{id}")
    public Genre getGenreById(@PathVariable Long id) { // Получаем жанр по ID
        return genreService.getGenreById(id); // Вызываем метод сервиса для получения жанра
    }
}


