package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryMpaRatingStorage implements MpaRatingStorage {

    private final Map<Long, MpaRating> ratings = new HashMap<>(); // Хранилище рейтингов в памяти, где ключ - ID

    @Override
    public List<MpaRating> getAllMpa() {
        // Возвращаем список всех рейтингов, хранящихся в Map
        return new ArrayList<>(ratings.values());
    }

    @Override
    public MpaRating getMpaById(Long id) {
        // Получаем рейтинг по ID из хранилища
        return ratings.get(id);
    }

}

