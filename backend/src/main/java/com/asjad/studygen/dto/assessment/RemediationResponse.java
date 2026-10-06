package com.asjad.studygen.dto.assessment;

import java.util.List;

public record RemediationResponse(
        Long moduleId,
        String moduleTitle,
        boolean wasFailed,
        Long attemptId,
        int originalScore,
        String diagnosis,
        String simplifiedExplanation,
        List<ConceptRemediationDTO> weakConcepts,
        Long remediationAssessmentId,
        List<QuestionResponse> retestQuestions
) {
    public RemediationResponse(
            Long attemptId,
            int originalScore,
            String diagnosis,
            String simplifiedExplanation,
            List<QuestionResponse> retestQuestions
    ) {
        this(null, null, true, attemptId, originalScore, diagnosis, simplifiedExplanation,
                List.of(), null, retestQuestions);
    }
}
