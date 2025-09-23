package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Genre {
    private Long id;       // Уникальный идентификатор жанра
    private String name;     // Название жанра
}
