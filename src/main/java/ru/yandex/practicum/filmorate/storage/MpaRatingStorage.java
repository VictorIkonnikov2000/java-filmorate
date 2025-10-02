package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.MpaRating;

import java.util.List;

public interface MpaRatingStorage {
    MpaRating getMpaRatingById(int id);


    List<MpaRating> getAllMpa();


    MpaRating getMpaById(Long id);
}

