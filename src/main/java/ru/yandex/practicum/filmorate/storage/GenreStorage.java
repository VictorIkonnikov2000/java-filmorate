package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GenreStorage {
    /**
     * Возвращает список всех жанров.
     *
     * @return Список объектов Genre.
     */
    List<Genre> getAllGenres();

    /**
     * Возвращает жанр по его идентификатору.
     *
     * @param id Идентификатор жанра.
     * @return Объект Genre, соответствующий заданному идентификатору, или null, если жанр не найден.
     */
    Optional<Genre> getGenreById(Long id);

    /**
     * Добавляет новый жанр.
     *
     * @param genre Жанр для добавления.
     * @return Добавленный Genre с присвоенным ID.
     */
    Genre addGenre(Genre genre);

    /**
     * Обновляет существующий жанр.
     *
     * @param genre Жанр для обновления.
     * @return Обновленный Genre.
     */
    Genre updateGenre(Genre genre);

    /**
     * Удаляет жанр по ID.
     *
     * @param id ID удаляемого жанра.
     */
    void deleteGenre(Long id);

    List<Genre> getGenresByIds(Collection<Long> genreIds);
}
