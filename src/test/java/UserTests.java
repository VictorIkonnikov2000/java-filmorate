import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserTests {

    private UserController userController;
    private User validUser;

    @BeforeEach
    void setUp() {
        userController = new UserController();
        validUser = new User();
        validUser.setEmaill("test@example.com");
        validUser.setLogin("testLogin");
        validUser.setName("testName");
        validUser.setBirthday(LocalDate.of(1990, 1, 1));
    }

    @Test
    void createUser_ValidUser_ReturnsCreatedStatus() {
        ResponseEntity<User> response = userController.createUser(validUser);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, ((List<User>) userController.getAllUsers().getBody()).size());
    }

    @Test
    void createUser_InvalidEmail_ReturnsBadRequestStatus() {
        User invalidUser = new User();
        invalidUser.setEmaill("invalid-email");
        invalidUser.setLogin("testLogin");
        ResponseEntity<User> response = userController.createUser(invalidUser);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

}

