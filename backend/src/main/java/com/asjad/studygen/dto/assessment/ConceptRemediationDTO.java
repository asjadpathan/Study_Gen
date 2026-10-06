package com.asjad.studygen.dto.assessment;

import java.util.List;

public record ConceptRemediationDTO(
        String conceptTitle,
        String gapDiagnosis,
        String simplifiedExplanation,
        String analogy,
        List<String> visualSteps,
        QuickCheckDTO quickCheck
) {}
