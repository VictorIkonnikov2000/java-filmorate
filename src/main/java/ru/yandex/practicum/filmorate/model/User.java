package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.LocalDate;
/**
 * Film.
 */

@Data
public class User {
    private Long id;
    private String emaill;
    private String login;
    private String name;
    private LocalDate birthday;
}
