package com.example.zuora.service;

import com.example.zuora.dto.ParqRequest;
import com.example.zuora.model.HealthQuestionnaire;
import com.example.zuora.model.User;
import com.example.zuora.repository.HealthQuestionnaireRepository;
import com.example.zuora.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for handling PAR-Q health questionnaire submissions
 */
@Service
public class HealthQuestionnaireService {

    private final HealthQuestionnaireRepository questionnaireRepository;
    private final UserRepository userRepository;

    public HealthQuestionnaireService(HealthQuestionnaireRepository questionnaireRepository,
                                      UserRepository userRepository) {
        this.questionnaireRepository = questionnaireRepository;
        this.userRepository = userRepository;
    }

    /**
     * Submit a PAR-Q questionnaire
     */
    @Transactional
    public HealthQuestionnaire submitParq(User user, ParqRequest request, HttpServletRequest httpRequest) {
        HealthQuestionnaire questionnaire = new HealthQuestionnaire();
        questionnaire.setUser(user);

        // Map PAR-Q responses
        questionnaire.setQ1HeartDisease(request.getQ1HeartDisease());
        questionnaire.setQ2ChestPainActivity(request.getQ2ChestPainActivity());
        questionnaire.setQ3ChestPainNoActivity(request.getQ3ChestPainNoActivity());
        questionnaire.setQ4BalanceIssues(request.getQ4BalanceIssues());
        questionnaire.setQ5BoneJointIssues(request.getQ5BoneJointIssues());
        questionnaire.setQ6BloodPressureMeds(request.getQ6BloodPressureMeds());
        questionnaire.setQ7OtherReasons(request.getQ7OtherReasons());
        questionnaire.setAdditionalNotes(request.getAdditionalNotes());

        questionnaire.setQuestionnaireType("PARQ");
        questionnaire.setVersion("2024-01");
        questionnaire.setIpAddress(httpRequest.getRemoteAddr());

        questionnaire = questionnaireRepository.save(questionnaire);

        // Update user's PAR-Q tracking
        user.setParqCompletedAt(LocalDateTime.now());
        user.setParqVersion("2024-01");
        userRepository.save(user);

        return questionnaire;
    }

    /**
     * Check if user has completed PAR-Q
     */
    public boolean hasCompletedParq(Long userId) {
        return questionnaireRepository.existsByUserId(userId);
    }

    /**
     * Get the latest PAR-Q for a user
     */
    public HealthQuestionnaire getLatestQuestionnaire(Long userId) {
        return questionnaireRepository.findFirstByUserIdOrderByCompletedAtDesc(userId).orElse(null);
    }

    /**
     * Get all PAR-Q submissions for a user
     */
    public List<HealthQuestionnaire> getQuestionnaireHistory(Long userId) {
        return questionnaireRepository.findByUserId(userId);
    }

    /**
     * Check if user requires medical clearance based on latest PAR-Q
     */
    public boolean requiresMedicalClearance(Long userId) {
        HealthQuestionnaire latest = getLatestQuestionnaire(userId);
        if (latest == null) {
            return true; // No PAR-Q means can't exercise
        }
        return latest.requiresMedicalClearance();
    }
}