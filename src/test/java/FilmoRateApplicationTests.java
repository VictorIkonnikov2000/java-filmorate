import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.FilmorateApplication; // Импорт класса приложения
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// Говорит Spring создать тестовый контекст для работы с БД
@AutoConfigureTestDatabase // Конфигурирует тестовую БД вместо основной
@RequiredArgsConstructor(onConstructor_ = @Autowired) // Создаёт конструктор с @Autowired
@Import({UserDbStorage.class})// Импортируем класс UserDbStorage в контекст тестирования
@SpringBootTest(classes = FilmorateApplication.class) // Указываем, какой класс использовать для конфигурации Spring Boot
class FilmoRateApplicationTests {

    private final UserDbStorage userStorage; // Инжектим UserDbStorage

    @Test
    public void testFindUserById() {
        // Создаем пользователя для теста.  ID устанавливать не нужно, его присвоит база данных.
        User user = User.builder()
                .email("test@example.com")
                .login("testLogin")
                .name("Test User")
                .birthday(java.time.LocalDate.of(1990, 1, 1))
                .build();
        userStorage.createUser(user); // Сохраняем пользователя в базу данных. После сохранения у него будет присвоен ID.

        Optional<User> userOptional = Optional.ofNullable(userStorage.getUserById(1L)); // Ищем пользователя по id.  Важно! Убедитесь, что в тестовой БД есть начальные данные.  Иначе тест упадет.

        assertThat(userOptional) // Проверяем, что Optional не пустой
                .isPresent()
                .hasValueSatisfying(user1 ->
                        assertThat(user1).hasFieldOrPropertyWithValue("id", 1L) // Проверяем, что id пользователя = 1
                );
    }
}

