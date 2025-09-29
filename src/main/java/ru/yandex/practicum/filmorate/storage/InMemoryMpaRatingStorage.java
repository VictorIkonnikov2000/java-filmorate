package ru.yandex.practicum.filmorate.storage;


import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("InMemoryMpaRatingStorage") // Указываем, что это компонент Spring для автоматического создания экземпляра
public class InMemoryMpaRatingStorage implements MpaRatingStorage {
    private final Map<Long, MpaRating> ratings = new HashMap<>(); // Хранилище рейтингов в памяти, где ключ - ID

    public InMemoryMpaRatingStorage() {
        // Инициализация MPA рейтингов.  Исправлено на соответствие типу Long и ratings
        ratings.put(1L, new MpaRating(1L, "G"));
        ratings.put(2L, new MpaRating(2L, "PG"));
        ratings.put(3L, new MpaRating(3L, "PG-13"));
        ratings.put(4L, new MpaRating(4L, "R"));
        ratings.put(5L, new MpaRating(5L, "NC-17"));
    }

    // Убедитесь, что этот метод больше не используется.  Если используется, нужно исправить.
    @Override
    public MpaRating getMpaRatingById(int id) {
        //  Этот метод устарел, потому что использует int id. Используйте getMpaById(Long id)
        throw new UnsupportedOperationException("Этот метод устарел. Используйте getMpaById(Long id)");
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
