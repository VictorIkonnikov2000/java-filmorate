import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FilmTests {

    @InjectMocks
    private FilmController controller;
    private Film validFilm;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new FilmController();
        //Инициализируем валидный Film
        validFilm = new Film(1L, "Valid Name", "Valid Description", 100, LocalDate.of(2000, 1, 1));
    }

    @Test
    void testCreateValidFilm_Returns201() {
        ResponseEntity<?> response = controller.createFilm(validFilm);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testCreateFilmWithInvalidData_Returns400() {
        //Создаем Film с пустым именем (невалидное состояние)
        Film invalidFilm = new Film(1L, "", "description", 100, LocalDate.of(2000, 1, 1));
        ResponseEntity<?> response = controller.createFilm(invalidFilm);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testUpdateExistingFilm_Returns200() {
        controller.createFilm(validFilm);
        Film updatedFilm = new Film(1L, "Updated Name", "Updated Description", 120, LocalDate.of(2001, 1, 1));
        ResponseEntity<?> response = controller.updateFilm(updatedFilm);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testUpdateNonExistingFilm_Returns404() {
        //Создаем Film с несуществующим ID
        Film nonExistingFilm = new Film(999L, "Name", "Description", 100, LocalDate.of(2000, 1, 1));
        ResponseEntity<?> response = controller.updateFilm(nonExistingFilm);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetAllFilms_Returns200() {
        controller.createFilm(validFilm);
        ResponseEntity<List<Film>> response = controller.getAllFilms();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty());
    }
}

