package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;


@Data
@Builder
public class Film {
   private Long id;@NotBlank(message = "Name cannot be blank")
   private String name;

   @Size(max = 200, message = "Description cannot exceed 200 characters")
   private String description;

   @NotNull(message = "Release date cannot be null")
   private LocalDate releaseDate;

   @Positive(message = "Duration must be positive")
   private Integer duration;

   private List<Genre> genres;

   @NotNull(message = "MPA Rating cannot be null")
   private MpaRating mpa;


   public void setGenres(List<Genre> genres) {
      this.genres = (genres != null) ? genres : Collections.emptyList();
   }

   public List<Genre> getGenres() {
      return (genres != null) ? genres : Collections.emptyList();
   }


   private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

   public void validate() throws ValidationException {
      if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
         throw new ValidationException("Release date cannot be before 1895-12-28");
      }
   }
}


