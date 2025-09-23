package ru.yandex.practicum.filmorate.model;

import lombok.Data;
import ru.yandex.practicum.filmorate.exception.ValidationException;

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


   public Film(Long id, String name, String description, LocalDate releaseDate, List<String> genres, Integer duration, MpaRating mpa) {
      this.id = id;
      this.name = name;
      this.description = description;
      this.releaseDate = releaseDate;
      this.genres = genres;
      this.duration = duration;
      this.mpa = mpa;
   }
   private static final int MAX_DESCRIPTION_LENGTH = 200;
   private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

   public void validate() throws ValidationException {
      if (name == null || name.isEmpty()) {
         throw new ValidationException("Название фильма не может быть пустым.");
      }

      if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
         throw new ValidationException("Описание фильма не может превышать 200 символов.");
      }

      if (releaseDate != null && releaseDate.isBefore(MIN_RELEASE_DATE)) {
         throw new ValidationException("Дата релиза фильма не может быть раньше 28 декабря 1895 года.");
      }

      if (duration <= 0) {
         throw new ValidationException("Продолжительность фильма должна быть положительной.");
      }

      if (mpa == null) {
         throw new ValidationException("MPA рейтинг не может быть пустым.");
      }

   }
}
