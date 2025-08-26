package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmorateApplicationTests {

    private FilmController controller;
    private Film film1, film2;

    @BeforeEach
    void setUp() {
        controller = new FilmController();
        film1 = new Film();
        film1.setName("Name1");
        film1.setDescription("Desc1");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(100);

        film2 = new Film();
        film2.setName("Name2");
        film2.setDescription("Desc2");
        film2.setReleaseDate(LocalDate.of(2010, 5, 5));
        film2.setDuration(120);
    }

    @Test
    void createValidFilm() {
        ResponseEntity<Film> response = controller.createFilm(film1);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody().getId());
    }

    @Test
    void createInvalidFilm_emptyName() {
        film1.setName("");
        assertThrows(ValidationException.class, () -> controller.createFilm(film1));
    }

    @Test
    void updateExistingFilm() {
        controller.createFilm(film1); // Сначала создаём фильм
        film1.setName("UpdatedName");
        ResponseEntity<Film> response = controller.updateFilm(film1);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("UpdatedName", response.getBody().getName());
    }

    @Test
    void updateNonExistingFilm() { // Проверяем, что будет, если пытаемся обновить несуществующий фильм
        film1.setId(999L); // ID, которого нет в списке фильмов
        ResponseEntity<Film> response = controller.updateFilm(film1);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getAllFilms() {
        controller.createFilm(film1);
        controller.createFilm(film2);
        ResponseEntity<List<Film>> response = controller.getAllFilms();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(2, response.getBody().size());
    }
}
