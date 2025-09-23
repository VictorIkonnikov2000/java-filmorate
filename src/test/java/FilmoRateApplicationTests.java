import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest // Говорит Spring создать тестовый контекст для работы с БД
@AutoConfigureTestDatabase // Конфигурирует тестовую БД вместо основной
@RequiredArgsConstructor(onConstructor_ = @Autowired) // Создаёт конструктор с @Autowired
@Import({UserDbStorage.class}) // Импортируем класс UserDbStorage в контекст тестирования
class FilmoRateApplicationTests {

    private final UserDbStorage userStorage; // Инжектим UserDbStorage

    @Test
    public void testFindUserById() {
        // Предполагаем, что в тестовой БД есть пользователь с id = 1 (инициализируется schema.sql)

        User user = User.builder()
                .email("test@example.com")
                .login("testLogin")
                .name("Test User")
                .birthday(java.time.LocalDate.of(1990, 1, 1))
                .build();
        userStorage.createUser(user);

        Optional<User> userOptional = Optional.ofNullable(userStorage.getUserById(1L)); // Ищем пользователя по id

        assertThat(userOptional) // Проверяем, что Optional не пустой
                .isPresent()
                .hasValueSatisfying(user1 ->
                        assertThat(user1).hasFieldOrPropertyWithValue("id", 1L) // Проверяем, что id пользователя = 1
                );
    }
}
