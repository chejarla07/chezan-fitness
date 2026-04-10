package com.example.zuora.repository;

import com.example.zuora.model.ParqQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParqQuestionRepository extends JpaRepository<ParqQuestion, Long> {

    List<ParqQuestion> findByActiveTrueOrderByDisplayOrderAsc();

    List<ParqQuestion> findAllByOrderByDisplayOrderAsc();

    Optional<ParqQuestion> findByQuestionNumber(Integer questionNumber);

    long countByActiveTrue();
}