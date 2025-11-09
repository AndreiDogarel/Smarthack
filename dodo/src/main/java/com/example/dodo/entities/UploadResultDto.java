package com.example.dodo.entities;
import jakarta.validation.constraints.*;
import java.util.List;

public class UploadResultDto {
    @NotBlank public String summary;
    @Size(min=1) public List<QuizDto> quizzes;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<QuizDto> getQuizzes() { return quizzes; }
    public void setQuizzes(List<QuizDto> quizzes) { this.quizzes = quizzes; }

    public static class QuizDto {
        @NotBlank public String question;
        @Size(min=2, max=10) public List<@NotBlank String> options;
        @Min(0) public int answer_index;
        public String hint;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public List<String> getOptions() { return options; }
        public void setOptions(List<String> options) { this.options = options; }
        public int getAnswer_index() { return answer_index; }
        public void setAnswer_index(int answer_index) { this.answer_index = answer_index; }
        public String getHint() { return hint; }
        public void setHint(String hint) { this.hint = hint; }
    }
}
