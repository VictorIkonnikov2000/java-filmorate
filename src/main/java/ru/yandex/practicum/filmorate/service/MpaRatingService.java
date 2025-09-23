package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.MpaRatingStorage; // Предполагаем, что есть интерфейс/класс для работы с данными MPA

import java.util.List;

@Service
@RequiredArgsConstructor
public class MpaRatingService {

    private final MpaRatingStorage mpaRatingStorage; // Класс для доступа к данным MPA

    public List<MpaRating> getAllMpa() {
        // Возвращает список всех рейтингов MPA, используя хранилище.
        return mpaRatingStorage.getAllMpa();
    }

    public MpaRating getMpaById(Long id) {
        // Возвращает рейтинг MPA по его ID, используя хранилище.
        // Если рейтинг не найден, хранилище должно вернуть null или выбросить исключение, которое здесь можно обработать.
        return mpaRatingStorage.getMpaById(id);
    }
}
