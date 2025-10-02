package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GenreStorage {


    List<Genre> getAllGenres();


    Optional<Genre> getGenreById(Long id);


    Genre addGenre(Genre genre);


    Genre updateGenre(Genre genre);


    void deleteGenre(Long id);

    List<Genre> getGenresByIds(Collection<Long> genreIds);
}
