package com.example.zuora.service;

import com.example.zuora.model.ParqQuestion;
import com.example.zuora.repository.ParqQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing PAR-Q questions
 */
@Service
public class ParqQuestionService {

    private final ParqQuestionRepository parqQuestionRepository;

    public ParqQuestionService(ParqQuestionRepository parqQuestionRepository) {
        this.parqQuestionRepository = parqQuestionRepository;
    }

    /**
     * Get all questions ordered by display order
     */
    public List<ParqQuestion> getAllQuestions() {
        return parqQuestionRepository.findAllByOrderByDisplayOrderAsc();
    }

    /**
     * Get active questions only
     */
    public List<ParqQuestion> getActiveQuestions() {
        return parqQuestionRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Get question by ID
     */
    public Optional<ParqQuestion> getQuestionById(Long id) {
        return parqQuestionRepository.findById(id);
    }

    /**
     * Create a new question
     */
    @Transactional
    public ParqQuestion createQuestion(Integer questionNumber, String questionText, String helpText,
                                        Boolean required, Integer displayOrder) {
        ParqQuestion question = new ParqQuestion();
        question.setQuestionNumber(questionNumber);
        question.setQuestionText(questionText);
        question.setHelpText(helpText);
        question.setRequired(required != null ? required : true);
        question.setActive(true);
        question.setDisplayOrder(displayOrder != null ? displayOrder : questionNumber);

        return parqQuestionRepository.save(question);
    }

    /**
     * Update a question
     */
    @Transactional
    public ParqQuestion updateQuestion(Long id, String questionText, String helpText,
                                         Boolean active, Boolean required, Integer displayOrder) {
        ParqQuestion question = parqQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        if (questionText != null) {
            question.setQuestionText(questionText);
        }
        if (helpText != null) {
            question.setHelpText(helpText);
        }
        if (active != null) {
            question.setActive(active);
        }
        if (required != null) {
            question.setRequired(required);
        }
        if (displayOrder != null) {
            question.setDisplayOrder(displayOrder);
        }

        return parqQuestionRepository.save(question);
    }

    /**
     * Delete a question
     */
    @Transactional
    public void deleteQuestion(Long id) {
        ParqQuestion question = parqQuestionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + id));

        parqQuestionRepository.delete(question);
    }

    /**
     * Reorder questions
     */
    @Transactional
    public void reorderQuestions(List<Long> questionIds) {
        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            ParqQuestion question = parqQuestionRepository.findById(questionId)
                    .orElseThrow(() -> new IllegalArgumentException("Question not found with id: " + questionId));
            question.setDisplayOrder(i + 1);
            parqQuestionRepository.save(question);
        }
    }

    /**
     * Initialize default PAR-Q questions if none exist
     */
    @Transactional
    public void initializeDefaultQuestions() {
        if (parqQuestionRepository.count() > 0) {
            return; // Questions already exist
        }

        // Standard PAR-Q questions
        createQuestion(1,
            "Has your doctor ever said that you have a heart condition OR that you should only do physical activity recommended by a doctor?",
            "If you have any heart conditions, please consult with your doctor before starting an exercise program.",
            true, 1);

        createQuestion(2,
            "Do you feel pain in your chest when you do physical activity?",
            "Chest pain during physical activity may indicate an underlying heart condition.",
            true, 2);

        createQuestion(3,
            "In the past month, have you had chest pain when you were NOT doing physical activity?",
            "Chest pain at rest is a serious symptom that should be evaluated by a doctor.",
            true, 3);

        createQuestion(4,
            "Do you lose your balance because of dizziness OR do you ever lose consciousness?",
            "Balance issues or loss of consciousness during exercise can be dangerous.",
            true, 4);

        createQuestion(5,
            "Do you have a bone or joint problem (for example, back, knee, or hip) that could be made worse by a change in your physical activity?",
            "Joint problems may require modified exercise programs.",
            true, 5);

        createQuestion(6,
            "Is your doctor currently prescribing medication for your blood pressure or heart condition?",
            "Some medications can affect exercise tolerance.",
            true, 6);

        createQuestion(7,
            "Do you know of ANY OTHER reason why you should not do physical activity?",
            "Any other medical conditions or concerns should be disclosed.",
            true, 7);
    }

    /**
     * Get count of active questions
     */
    public long getActiveQuestionCount() {
        return parqQuestionRepository.countByActiveTrue();
    }
}