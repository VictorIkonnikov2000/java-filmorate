package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;


@Data
public class Film {
   Long id;
   String name;
   String description;
   LocalDate releaseDate;
   Integer duration;
   List<String> genres; // Добавляем поле для хранения списка жанров
   MpaRating mpa;

   public Film(Long id, String name, String description, Integer duration, LocalDate releaseDate) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.duration = duration;
      this.releaseDate = releaseDate;
   }

   public Film(Long id, String name, String description, LocalDate releaseDate, List<String> genres, Integer duration, MpaRating mpa) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.releaseDate = releaseDate;
      this.genres = genres;
      this.duration = duration;
      this.mpa = mpa;
   }
}
