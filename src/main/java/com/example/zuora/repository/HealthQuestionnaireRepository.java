package com.example.zuora.repository;

import com.example.zuora.model.HealthQuestionnaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HealthQuestionnaireRepository extends JpaRepository<HealthQuestionnaire, Long> {

    /**
     * Get the most recent PAR-Q for a user
     */
    Optional<HealthQuestionnaire> findFirstByUserIdOrderByCompletedAtDesc(Long userId);

    /**
     * Get all PAR-Q submissions for a user
     */
    List<HealthQuestionnaire> findByUserId(Long userId);

    /**
     * Check if user has completed any PAR-Q
     */
    boolean existsByUserId(Long userId);

    /**
     * Get all PAR-Qs for a specific version
     */
    List<HealthQuestionnaire> findByVersion(String version);
}