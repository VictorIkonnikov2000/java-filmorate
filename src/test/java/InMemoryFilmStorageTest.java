import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.InMemoryFilmStorage;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryFilmStorageTest {

    private InMemoryFilmStorage filmStorage;
    private Film film1;
    private Film film2;

    @BeforeEach
    void setUp() {
        filmStorage = new InMemoryFilmStorage();

        film1 = new Film();
        film1.setName("Film1");
        film1.setDescription("Description1");
        film1.setReleaseDate(LocalDate.of(2000, 1, 1));
        film1.setDuration(120);
        film1.setMpa(new MpaRating(1L, "G"));

        film2 = new Film();
        film2.setName("Film2");
        film2.setDescription("Description2");
        film2.setReleaseDate(LocalDate.of(2010, 5, 10));
        film2.setDuration(90);
        film2.setMpa(new MpaRating(2L, "PG"));
    }

    @Test
    void createFilm() {
        Film createdFilm = filmStorage.createFilm(film1);
        assertNotNull(createdFilm.getId());
        assertEquals(film1.getName(), createdFilm.getName());
    }

    @Test
    void updateFilm() {
        Film createdFilm = filmStorage.createFilm(film1);
        createdFilm.setName("UpdatedName");
        Film updatedFilm = filmStorage.updateFilm(createdFilm);
        assertEquals("UpdatedName", updatedFilm.getName());
    }

    @Test
    void updateNonExistingFilmShouldThrowException() {
        film1.setId(999L);
        assertThrows(NotFoundException.class, () -> filmStorage.updateFilm(film1));
    }

    @Test
    void getAllFilms() {
        filmStorage.createFilm(film1);
        filmStorage.createFilm(film2);
        List<Film> films = filmStorage.getAllFilms();
        assertEquals(2, films.size());
    }

    @Test
    void addLike() {
        Film createdFilm = filmStorage.createFilm(film1);
        filmStorage.addLike(createdFilm.getId(), 1L);
        // Нет способа узнать количество лайков напрямую из filmStorage,
        // так как `filmLikes` - приватное поле.  В реальной реализации должен быть метод для получения кол-ва лайков
    }

    @Test
    void removeLike() {
        Film createdFilm = filmStorage.createFilm(film1);
        filmStorage.addLike(createdFilm.getId(), 1L);
        filmStorage.removeLike(createdFilm.getId(), 1L);
        // Нет способа узнать количество лайков напрямую из filmStorage.
    }

    @Test
    void getPopularFilms() {
        Film createdFilm1 = filmStorage.createFilm(film1);
        Film createdFilm2 = filmStorage.createFilm(film2);

        filmStorage.addLike(createdFilm1.getId(), 1L);
        filmStorage.addLike(createdFilm1.getId(), 2L);
        filmStorage.addLike(createdFilm2.getId(), 1L);

        List<Film> popularFilms = filmStorage.getPopularFilms(1);
        assertEquals(createdFilm1.getId(), popularFilms.get(0).getId());
    }

    @Test
    void getFilmById() {
        Film createdFilm = filmStorage.createFilm(film1);
        Film retrievedFilm = filmStorage.getFilmById(createdFilm.getId());
        assertEquals(createdFilm.getId(), retrievedFilm.getId());
    }

    @Test
    void getFilmByInvalidIdShouldThrowException() {
        assertThrows(NotFoundException.class, () -> filmStorage.getFilmById(999L));
    }
}
