package ru.yandex.practicum.filmorate.model;

public enum MpaRating {
    G(1),
    PG(2),
    PG13(3),
    R(4),
    NC17(5);

    private final int id; // Добавляем поле для хранения ID

    MpaRating(int id) { // Создаем конструктор для установки ID
        this.id = id;
    }

    public int getId() { // Создаем метод для получения ID
        return id;
    }
}

