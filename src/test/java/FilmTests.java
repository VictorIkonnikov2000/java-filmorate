import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class FilmTests {

    @InjectMocks
    private FilmController filmController;

    @Mock // Создаём мок FilmService
    private FilmService filmService;

    private Film film;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Инициализируем моки
        film = new Film(1L, "Film Name", "Film Description", 120, LocalDate.of(2023, 1, 1));
    }


    @Test
    void getAllFilms_shouldReturnOk() {
        when(filmService.getAllFilms()).thenReturn(new ResponseEntity<>(Collections.singletonList(film), HttpStatus.OK)); // Мокируем поведение filmService
        ResponseEntity<List<Film>> response = filmController.getAllFilms();
        assertEquals(HttpStatus.OK, response.getStatusCode()); // Проверяем статус код
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());//Проверяем размер списка
        assertEquals(film, response.getBody().get(0));//Проверяем что фильм тот же
    }
}



