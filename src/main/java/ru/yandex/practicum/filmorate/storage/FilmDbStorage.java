package ru.yandex.practicum.filmorate.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
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

        // Получаем ID сгенерированный базой данных.  Важно:  зависит от СУБД и настроек.  Здесь упрощенный вариант.
        String sqlQuery = "SELECT film_id FROM films WHERE name = ? AND description = ? AND release_date = ? AND duration = ? AND mpa_id = ?";

        List<Long> filmIdList = jdbcTemplate.query(sqlQuery, (rs, rowNum) -> rs.getLong("film_id"), film.getName(), film.getDescription(), Date.valueOf(film.getReleaseDate()), film.getDuration(), film.getMpa().getId());

        //Проверка, что filmIdList не пустой и берем первый id
        Long filmId = filmIdList.stream().findFirst().orElse(null);
        if (filmId != null){
            film.setId(filmId);
            return film;
        } else {
            throw new NotFoundException("Film not found after creation");
        }

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
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_id FROM films";
        return jdbcTemplate.query(sql, filmRowMapper());
    }

    @Override
    public void addLike(Long filmId, Long userId) {
        String sql = "INSERT INTO likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id " +
                "FROM films f " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id " +
                "ORDER BY COUNT(l.user_id) DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, filmRowMapper(), count);
    }

    @Override
    public Film getFilmById(Long filmId) {
        String sql = "SELECT film_id, name, description, release_date, duration, mpa_id FROM films WHERE film_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, filmRowMapper(), filmId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new NotFoundException("Film not found");
        }
    }

    private RowMapper<Film> filmRowMapper() {
        return (rs, rowNum) -> {
            Film film = new Film();
            film.setId(rs.getLong("film_id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));
            film.setReleaseDate(rs.getDate("release_date").toLocalDate());
            film.setDuration(rs.getInt("duration"));
            MpaRating mpa = new MpaRating(rs.getLong("mpa_id"), null);
            film.setMpa(mpa);
            return film;
        };
    }
}

