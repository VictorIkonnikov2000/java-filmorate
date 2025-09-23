package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

public interface MpaRatingStorage {
    /**
     * Получает список всех рейтингов MPA.
     *
     * @return Список объектов MpaRating.
     */
    List<MpaRating> getAllMpa();

    /**
     * Получает рейтинг MPA по его идентификатору.
     *
     * @param id Идентификатор рейтинга MPA.
     * @return Объект MpaRating, соответствующий заданному идентификатору, или null, если рейтинг не найден.
     */
    MpaRating getMpaById(Long id);
}

