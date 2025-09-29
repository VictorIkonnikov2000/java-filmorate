package ru.yandex.practicum.filmorate.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;



@Slf4j
@Component("InMemoryFilmStorage")
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private final Map<Long, Set<Long>> filmLikes = new HashMap<>();
    private Long filmIdCounter = 1L;

    @Override
    public Film createFilm(Film film) {
        log.info("Creating film in storage: {}", film);

        // Валидация MPA и жанров (теперь вызывается из Film)
        try {
            film.validate();
        } catch (ValidationException e) {
            log.error("Film validation failed: {}", e.getMessage());
            throw e; // пробрасываем исключение
        }

        film.setId(filmIdCounter++);
        films.put(film.getId(), film);
        log.info("Film added to storage: {}", film);
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        try {
            film.validate();
        } catch (ValidationException e) {
            throw e; // пробрасываем исключение
        }
        if (!films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id " + film.getId() + " не найден.");
        }
        films.put(film.getId(), film);
        log.info("Обновлен фильм: {}", film);
        return film;
    }

    @Override
    public List<Film> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов.");
        return new ArrayList<>(films.values());
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Фильм с id " + filmId + " не найден.");
        }
        //Если для фильма еще нет лайков, создаем новый set
        filmLikes.computeIfAbsent(filmId, k -> new HashSet<>());
        filmLikes.get(filmId).add(userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Фильм с id " + filmId + " не найден.");
        }
        //Если для фильма еще нет лайков, создаем новый set (на всякий случай)
        filmLikes.computeIfAbsent(filmId, k -> new HashSet<>());
        filmLikes.get(filmId).remove(userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        return films.values().stream()
                .sorted((f1, f2) -> Integer.compare(Optional.ofNullable(filmLikes.get(f2.getId())).map(Set::size).orElse(0),
                        Optional.ofNullable(filmLikes.get(f1.getId())).map(Set::size).orElse(0)))
                .limit(count)
                .collect(Collectors.toList());
    }

    @Override
    public Film getFilmById(Long filmId) {
        if (!films.containsKey(filmId)) {
            throw new NotFoundException("Film with id " + filmId + " not found.");
        }
        return films.get(filmId);
    }
}