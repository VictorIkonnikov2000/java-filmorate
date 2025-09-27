
-- Добавление MPA рейтингов
INSERT INTO mpa (name) VALUES ('G')
ON CONFLICT (name) DO NOTHING;

INSERT INTO mpa (name) VALUES ('PG')
ON CONFLICT (name) DO NOTHING;

INSERT INTO mpa (name) VALUES ('PG-13')
ON CONFLICT (name) DO NOTHING;

INSERT INTO mpa (name) VALUES ('R')
ON CONFLICT (name) DO NOTHING;

INSERT INTO mpa (name) VALUES ('NC-17')
ON CONFLICT (name) DO NOTHING;

-- Добавление Жанров
INSERT INTO genres (name) VALUES ('Комедия')
ON CONFLICT (name) DO NOTHING;

INSERT INTO genres (name) VALUES ('Драма')
ON CONFLICT (name) DO NOTHING;

INSERT INTO genres (name) VALUES ('Мультфильм')
ON CONFLICT (name) DO NOTHING;

INSERT INTO genres (name) VALUES ('Триллер')
ON CONFLICT (name) DO NOTHING;

INSERT INTO genres (name) VALUES ('Документальный')
ON CONFLICT (name) DO NOTHING;

INSERT INTO genres (name) VALUES ('Боевик')
ON CONFLICT (name) DO NOTHING;
