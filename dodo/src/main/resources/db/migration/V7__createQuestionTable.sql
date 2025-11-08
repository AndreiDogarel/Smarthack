CREATE TABLE questions (
   id SERIAL PRIMARY KEY,
   question_text VARCHAR(500) NOT NULL UNIQUE,
   variant_a VARCHAR(500) NOT NULL,
   variant_b VARCHAR(500) NOT NULL,
   variant_c VARCHAR(500) NOT NULL,
   variant_d VARCHAR(500) NOT NULL,
   correct_answer VARCHAR(500) NOT NULL
);