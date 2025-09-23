package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class MpaRating {
    private int id;
    private String name;

    public MpaRating(int id, String name) {
        this.id = id;
        this.name = name;
    }
}