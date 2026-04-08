package com.example.zuora.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PAR-Q (Physical Activity Readiness Questionnaire) entity
 * Standard health questionnaire required before gym membership activation
 */
@Entity
@Table(name = "health_questionnaires")
public class HealthQuestionnaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // PAR-Q Standard 7 Questions (all boolean - true means "YES" which indicates risk)
    @Column(name = "q1_heart_disease", nullable = false)
    private Boolean q1HeartDisease; // Has a doctor ever said you have heart disease?

    @Column(name = "q2_chest_pain_activity", nullable = false)
    private Boolean q2ChestPainActivity; // Do you feel chest pain during physical activity?

    @Column(name = "q3_chest_pain_no_activity", nullable = false)
    private Boolean q3ChestPainNoActivity; // Do you feel chest pain when NOT doing physical activity?

    @Column(name = "q4_balance_issues", nullable = false)
    private Boolean q4BalanceIssues; // Do you lose balance because of dizziness or lose consciousness?

    @Column(name = "q5_bone_joint_issues", nullable = false)
    private Boolean q5BoneJointIssues; // Do you have a bone or joint problem that could worsen?

    @Column(name = "q6_blood_pressure_meds", nullable = false)
    private Boolean q6BloodPressureMeds; // Are you on medication for blood pressure or heart condition?

    @Column(name = "q7_other_reasons", nullable = false)
    private Boolean q7OtherReasons; // Do you know of any other reason you should not exercise?

    // Additional Health Information
    @Column(columnDefinition = "TEXT")
    private String additionalNotes;

    // Metadata
    @Column(name = "questionnaire_type", nullable = false)
    private String questionnaireType = "PARQ"; // For future extensibility

    @Column(name = "version")
    private String version = "2024-01"; // PAR-Q version

    @Column(name = "completed_at", nullable = false)
    private LocalDateTime completedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @PrePersist
    protected void onCreate() {
        completedAt = LocalDateTime.now();
    }

    // ==================== HELPER METHODS ====================

    /**
     * Returns true if all answers are "NO" (safe to exercise without medical clearance)
     */
    public boolean isSafeToExercise() {
        return !q1HeartDisease && !q2ChestPainActivity && !q3ChestPainNoActivity
            && !q4BalanceIssues && !q5BoneJointIssues && !q6BloodPressureMeds
            && !q7OtherReasons;
    }

    /**
     * Returns true if any answer is "YES" (requires medical clearance)
     */
    public boolean requiresMedicalClearance() {
        return q1HeartDisease || q2ChestPainActivity || q3ChestPainNoActivity
            || q4BalanceIssues || q5BoneJointIssues || q6BloodPressureMeds
            || q7OtherReasons;
    }

    // ==================== GETTERS AND SETTERS ====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Boolean getQ1HeartDisease() { return q1HeartDisease; }
    public void setQ1HeartDisease(Boolean q1HeartDisease) { this.q1HeartDisease = q1HeartDisease; }

    public Boolean getQ2ChestPainActivity() { return q2ChestPainActivity; }
    public void setQ2ChestPainActivity(Boolean q2ChestPainActivity) { this.q2ChestPainActivity = q2ChestPainActivity; }

    public Boolean getQ3ChestPainNoActivity() { return q3ChestPainNoActivity; }
    public void setQ3ChestPainNoActivity(Boolean q3ChestPainNoActivity) { this.q3ChestPainNoActivity = q3ChestPainNoActivity; }

    public Boolean getQ4BalanceIssues() { return q4BalanceIssues; }
    public void setQ4BalanceIssues(Boolean q4BalanceIssues) { this.q4BalanceIssues = q4BalanceIssues; }

    public Boolean getQ5BoneJointIssues() { return q5BoneJointIssues; }
    public void setQ5BoneJointIssues(Boolean q5BoneJointIssues) { this.q5BoneJointIssues = q5BoneJointIssues; }

    public Boolean getQ6BloodPressureMeds() { return q6BloodPressureMeds; }
    public void setQ6BloodPressureMeds(Boolean q6BloodPressureMeds) { this.q6BloodPressureMeds = q6BloodPressureMeds; }

    public Boolean getQ7OtherReasons() { return q7OtherReasons; }
    public void setQ7OtherReasons(Boolean q7OtherReasons) { this.q7OtherReasons = q7OtherReasons; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }

    public String getQuestionnaireType() { return questionnaireType; }
    public void setQuestionnaireType(String questionnaireType) { this.questionnaireType = questionnaireType; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}