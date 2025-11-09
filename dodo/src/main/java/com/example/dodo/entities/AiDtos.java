package com.example.dodo.entities;

public class AiDtos {
    public static class HintRequest {
        public String question;
    }
    public static class HintResponse {
        public String hint;
    }
    public static class GenRequest {
        public String text;
        public Integer max_questions;
    }
    public static class QuizItem {
        public String question;
        public java.util.List<String> options;
        public Integer answer_index;
        public String hint;
    }
    public static class GenResponse {
        public String summary;
        public java.util.List<QuizItem> quizzes;
    }
}

