import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test; // Подключаем аннотации JUnit для тестов
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase; // Для настройки тестовой базы данных
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest; // Для работы с JDBC в тестах
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat; // Для удобных проверок в тестах

@JdbcTest // Говорит Spring создать контекст для JDBC-тестов
@AutoConfigureTestDatabase // Конфигурирует тестовую БД вместо основной
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
@SpringBootTest
class FilmoRateApplicationTests {

    private final UserDbStorage userStorage;

    @Test
    void testFindUserById() {
        //  В UserStorage#getUserById требует Long id
        // Чтобы тест работал надо передать 1L в качестве id
        Optional<User> userOptional = Optional.ofNullable(userStorage.getUserById(1L)); // Использование Optional.OfNullable
        // Проверяем, что вернулось из метода UserStorage
        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user ->
                        assertThat(user.getId()).isEqualTo(1L) // Явное указание типа Long в Assertions
                );
    }
}
