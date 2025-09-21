# java-filmorate
Template repository for Filmorate project.
<img width="719" height="496" alt="database_schema" src="https://github.com/user-attachments/assets/77a16f0e-14cc-4355-bd2a-b5e0d0e6413a" />

# Схема базы данных Filmorate
Эта схема представляет структуру базы данных Filmorate.  Она включает таблицы для хранения информации о фильмах, пользователях, жанрах, лайках и друзьях.  Ключевые таблицы: `films`, `users`, `genres`, `film_genres`, `likes`, `friends`.

## Примеры запросов

*   Получить фильм по ID:

    ```sql
    SELECT * FROM films WHERE id = 1;
    ```

*   Получить всех пользователей:

    ```sql
    SELECT * FROM users;
    ```

*   Добавить лайк фильму:

    ```sql
    INSERT INTO likes (film_id, user_id) VALUES (1, 2);
    ```

*   Получить список друзей пользователя:

    ```sql
    SELECT u.* FROM users u JOIN friends f ON u.id = f.friend_id WHERE f.user_id = 1 AND f.status = 'подтверждена';
    ```
