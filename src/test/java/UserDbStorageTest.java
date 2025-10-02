import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;

import java.time.LocalDate;


import static org.assertj.core.api.Assertions.assertThat;


@JdbcTest
@ContextConfiguration(classes = UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class UserDbStorageTest {

    private final UserStorage userStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM friends");
        jdbcTemplate.update("DELETE FROM users");
    }


    private User createTestUser(String email, String login, String name, LocalDate birthday) {
        User user = User.builder()
                .email(email)
                .login(login)
                .name(name)
                .birthday(birthday)
                .build();
        return userStorage.createUser(user);
    }

    @Test
    void testCreateUser() {
        User userToCreate = User.builder()
                .email("test@example.com")
                .login("test_user_login")
                .name("Test User Name")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User createdUser = userStorage.createUser(userToCreate);

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getId()).isPositive();
        assertThat(createdUser.getEmail()).isEqualTo("test@example.com");
        assertThat(createdUser.getLogin()).isEqualTo("test_user_login");
        assertThat(createdUser.getName()).isEqualTo("Test User Name");
        assertThat(createdUser.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));


        User foundUser = userStorage.getUserById(createdUser.getId());
        assertThat(foundUser).isEqualTo(createdUser);
    }
}
