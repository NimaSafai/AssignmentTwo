DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id INTEGER PRIMARY KEY,
    username TEXT UNIQUE,
    password TEXT
);

DROP TABLE IF EXISTS quiz;

CREATE TABLE quiz (
    id INTEGER PRIMARY KEY,
    user_id INTEGER,
    title TEXT,
    datetime TEXT,
    -- 1 if the quiz is public, 0 if it should just be visible to the author.
    public INTEGER,
    FOREIGN KEY(user_id) REFERENCES user(id)
);

DROP TABLE IF EXISTS question;

CREATE TABLE question (
    id INTEGER PRIMARY KEY,
    quiz_id INTEGER,
    -- The order of this question within its quiz.
    number INTEGER,
    -- Prompt is the actual question. For example: "What is the capital of Sweden?"
    prompt TEXT,
    -- Each option has its own column, so all questions are limited to exactly four options.
    option_1 TEXT,
    option_2 TEXT,
    option_3 TEXT,
    option_4 TEXT,
    -- The number of the correct option. Must be 1-4, but database does not guarantee this.
    correct_option INTEGER,
    -- Path to the image associated with this question, relative to the "images" folder.
    image_path TEXT,
    FOREIGN KEY(quiz_id) REFERENCES quiz(id)
);

INSERT INTO user(id, username, password) VALUES
(1, 'Brad', 'secret123'),
(2, 'Angelina', 'unhackabl3'),
(3, 'Will', 'i4mlegend');

INSERT INTO quiz(id, user_id, title, datetime, public) VALUES
(1, 1, 'Capital Cities', '2020-01-01T12:00:00', 1),
(2, 1, 'Population', '2020-01-02T12:00:00', 1),
(3, 2, 'Actors', '2020-01-03T12:00:00', 1),
(4, 3, 'Heads of State', '2020-01-04T12:00:00', 1),
(5, 3, 'Secret Identities', '2020-01-05T12:00:00', 0);

INSERT INTO question(quiz_id, number, prompt, option_1, option_2, option_3, option_4, correct_option, image_path) VALUES
(1, 1, 'What is the capital of Sweden?', 'Gothenburg', 'Stockholm', 'Malmo', 'Uppsala', 2, 'sweden.svg'),
(1, 2, 'What is the capital of Poland?', 'Warsaw', 'Krakow', 'Gdansk', 'Wroclaw', 1, 'poland.svg'),
(1, 3, 'What is the capital of Seychelles?', 'Anse Boileau', 'Beau Vallon', 'Takamaka', 'Victoria', 4, 'seychelles.svg'),
(1, 4, 'What is the capital of Iran?', 'Shiraz', 'Isfahan', 'Tehran', 'Qom', 3, 'iran.svg'),
(2, 1, 'What is the population of Sweden?', '7 million', '8 million', '9 million', '10 million', 4, 'sweden.svg'),
(2, 2, 'What is the population of Iran?', '73 million', '83 million', '93 million', '103 million', 2, 'iran.svg'),
(2, 3, 'What is the population of Poland?', '38 million', '43 million', '48 million', '53 million', 1, 'poland.svg'),
(3, 1, 'Which actor is from the United States?', 'Rachel McAdams', 'Ryan Gosling', 'Donald Sutherland', 'Anne Hathaway', 4, 'usa.svg'),
(3, 2, 'Which actor is from Iran?', 'Aishwarya Rai', 'Golshifteh Farahani', 'Amr Waked', 'Saba Mubarak', 2, 'iran.svg'),
(4, 1, 'Who is president of Iran?', 'Hassan Rouhani', 'Mohammad Khatami', 'Mahmoud Ahmadinejad', 'Abolhassan Banisadr', 1, 'iran.svg'),
(4, 2, 'Who is president of the United States?', 'Donald Trump', 'Sherman Klump', 'Donald Duck', 'Ronald McDonald', 1, 'usa.svg'),
(4, 3, 'Who is president of Poland?', 'Lech Kaczynski', 'Lech Walesa', 'Andrzej Duda', 'Bronislaw Komorowski', 3, 'poland.svg'),
(4, 4, 'Who is president of Seychelles?', 'James Michel', 'France-Albert Rene', 'Danny Faure', 'James Mancham', 3, 'seychelles.svg'),
(5, 1, 'Where is my secret summer house?', 'Marseille', 'Paris', 'Cannes', 'Lyon', 1, 'france.svg'),
(5, 2, 'Where is the top-secret real-world Men in Black headquarters located?', 'New York', 'California', 'Nevada', 'Idaho', 3, 'usa.svg'),
(5, 3, 'Where is the actual mothership from Independence Day being kept?', 'England', 'France', 'Russia', 'Sweden', 4, 'eu.svg');