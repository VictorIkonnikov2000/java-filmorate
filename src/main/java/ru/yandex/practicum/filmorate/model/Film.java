package ru.yandex.practicum.filmorate.model;

import lombok.Data;

import java.time.LocalDate;


@Data
public class Film {
   Long id;
   String name;
   String description;
   LocalDate releaseDate;
   Integer duration;

   public Film(Long id, String name, String description, Integer duration, LocalDate releaseDate) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.duration = duration;
      this.releaseDate = releaseDate;
   }
}
