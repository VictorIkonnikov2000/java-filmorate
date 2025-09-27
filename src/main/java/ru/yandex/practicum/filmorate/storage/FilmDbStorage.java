package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.validate.FilmValidate;


import java.util.List;
import java.sql.Date;


@Component
@Qualifier("filmDbStorage")
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public Film createFilm(Film film) {
        if (!FilmValidate.validateFilm(film)) {
            throw new IllegalArgumentException("Film validation failed");
        }
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()), film.getDuration(), film.getMpa().getId());
        return film; // Возвращаем созданный фильм
    }

    @Override
    public Film updateFilm(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE film_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()), film.getDuration(), film.getMpa().getId(), film.getId());

        if (rowsAffected == 0) {
            throw new NotFoundException("Film not found");
        }
        return film;
    }

    @Override
    public List<Film> getAllFilms() {
        //TODO: Реализация получения всех фильмов из БД
        return null;
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        //TODO: Реализация добавления лайка фильму в БД

    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        //TODO: Реализация удаления лайка фильму из БД
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        //TODO: Реализация получения популярных фильмов из БД
        return null;
    }

    @Override
    public Film getFilmById(Long filmId) {
        //TODO: Реализация получения фильма по ID из БД
        return null; //implementation later
    }
}

