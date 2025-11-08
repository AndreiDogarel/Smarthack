package com.example.dodo.repository;

import com.example.dodo.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionsRepository extends JpaRepository<Question, Integer> {
    @Query("SELECT q.correctAnswer FROM Question q WHERE q.id = :questionId")
    String getCorrectAnswerByQuestionId(@Param("questionId") Long questionId);

    List<Question> getQuestionsByDomain(String domain);
}
