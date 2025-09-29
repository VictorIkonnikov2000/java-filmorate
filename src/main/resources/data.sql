INSERT INTO mpa_ratings (mpa_id, name) VALUES (1, 'G') ON CONFLICT (mpa_id) DO NOTHING;
INSERT INTO mpa_ratings (mpa_id, name) VALUES (2, 'PG') ON CONFLICT (mpa_id) DO NOTHING;
INSERT INTO mpa_ratings (mpa_id, name) VALUES (3, 'PG-13') ON CONFLICT (mpa_id) DO NOTHING;
INSERT INTO mpa_ratings (mpa_id, name) VALUES (4, 'R') ON CONFLICT (mpa_id) DO NOTHING;
INSERT INTO mpa_ratings (mpa_id, name) VALUES (5, 'NC-17') ON CONFLICT (mpa_id) DO NOTHING;

-- Вставка начальных данных для жанров
-- Эти данные также являются справочными и используются для классификации фильмов.
INSERT INTO genres (genre_id, name) VALUES (1, 'Комедия') ON CONFLICT (genre_id) DO NOTHING;
INSERT INTO genres (genre_id, name) VALUES (2, 'Драма') ON CONFLICT (genre_id) DO NOTHING;
INSERT INTO genres (genre_id, name) VALUES (3, 'Мультфильм') ON CONFLICT (genre_id) DO NOTHING;
INSERT INTO genres (genre_id, name) VALUES (4, 'Триллер') ON CONFLICT (genre_id) DO NOTHING;
INSERT INTO genres (genre_id, name) VALUES (5, 'Документальный') ON CONFLICT (genre_id) DO NOTHING;
INSERT INTO genres (genre_id, name) VALUES (6, 'Боевик') ON CONFLICT (genre_id) DO NOTHING;
