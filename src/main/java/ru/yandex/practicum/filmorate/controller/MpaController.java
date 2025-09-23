package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.service.MpaRatingService;

import java.util.List;

@RestController
@RequiredArgsConstructor // Создает конструктор с @Autowired для полей, помеченных final
public class MpaController {

    private final MpaRatingService mpaRatingService; // Сервис для работы с рейтингами MPA

    @GetMapping("/mpa")
    public List<MpaRating> getAllMpa() { // Получаем список всех рейтингов MPA
        return mpaRatingService.getAllMpa(); // Вызываем метод сервиса для получения списка
    }

    @GetMapping("/mpa/{id}")
    public MpaRating getMpaById(@PathVariable Long id) { // Получаем рейтинг MPA по ID
        return mpaRatingService.getMpaById(id); // Вызываем метод сервиса для получения рейтинга
    }
}