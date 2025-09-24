package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.MpaRatingStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MpaRatingService {

    private final MpaRatingStorage mpaRatingStorage;

    public List<MpaRating> getAllMpa() {
        return mpaRatingStorage.getAllMpa();
    }

    public MpaRating getMpaById(Long id) {
        MpaRating mpaRating = mpaRatingStorage.getMpaById(id);
        if (mpaRating == null) {
            throw new NotFoundException("MpaRating with id=" + id + " not found");
        }
        return mpaRating;
    }
}

