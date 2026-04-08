package com.example.zuora.dto;

import jakarta.validation.constraints.NotNull;

/**
 * PAR-Q (Physical Activity Readiness Questionnaire) request DTO
 * Standard 7-question health questionnaire for gym membership
 */
public class ParqRequest {

    // Question 1: Has a doctor ever said you have heart disease?
    @NotNull(message = "Question 1 is required")
    private Boolean q1HeartDisease;

    // Question 2: Do you feel chest pain during physical activity?
    @NotNull(message = "Question 2 is required")
    private Boolean q2ChestPainActivity;

    // Question 3: Do you feel chest pain when NOT doing physical activity?
    @NotNull(message = "Question 3 is required")
    private Boolean q3ChestPainNoActivity;

    // Question 4: Do you lose balance because of dizziness or lose consciousness?
    @NotNull(message = "Question 4 is required")
    private Boolean q4BalanceIssues;

    // Question 5: Do you have a bone or joint problem that could worsen with exercise?
    @NotNull(message = "Question 5 is required")
    private Boolean q5BoneJointIssues;

    // Question 6: Are you on medication for blood pressure or a heart condition?
    @NotNull(message = "Question 6 is required")
    private Boolean q6BloodPressureMeds;

    // Question 7: Do you know of any other reason you should not exercise?
    @NotNull(message = "Question 7 is required")
    private Boolean q7OtherReasons;

    // Optional additional notes
    private String additionalNotes;

    // ==================== HELPER METHODS ====================

    /**
     * Returns true if any answer is "YES" (requires medical clearance)
     */
    public boolean hasAnyYesAnswer() {
        return Boolean.TRUE.equals(q1HeartDisease)
            || Boolean.TRUE.equals(q2ChestPainActivity)
            || Boolean.TRUE.equals(q3ChestPainNoActivity)
            || Boolean.TRUE.equals(q4BalanceIssues)
            || Boolean.TRUE.equals(q5BoneJointIssues)
            || Boolean.TRUE.equals(q6BloodPressureMeds)
            || Boolean.TRUE.equals(q7OtherReasons);
    }

    /**
     * Returns true if all answers are "NO" (safe to exercise)
     */
    public boolean isSafeToExercise() {
        return !hasAnyYesAnswer();
    }

    // ==================== GETTERS AND SETTERS ====================

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
}