package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryMpaRatingStorage implements MpaRatingStorage {
    private final Map<Integer, MpaRating> mpaRatings = new HashMap<>();
    private final Map<Long, MpaRating> ratings = new HashMap<>(); // Хранилище рейтингов в памяти, где ключ - ID

    public InMemoryMpaRatingStorage() {
        // Инициализация MPA рейтингов
        mpaRatings.put(1, new MpaRating(1, "G"));
        mpaRatings.put(2, new MpaRating(2, "PG"));
        mpaRatings.put(3, new MpaRating(3, "PG13"));
        mpaRatings.put(4, new MpaRating(4, "R"));
        mpaRatings.put(5, new MpaRating(5, "NC17"));
    }

    @Override
    public MpaRating getMpaRatingById(int id) {
        return mpaRatings.get(id);
    }

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

