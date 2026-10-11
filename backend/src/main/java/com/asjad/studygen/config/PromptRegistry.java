package com.asjad.studygen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * PromptRegistry — Centralized AI Prompt Provider
 *
 * <p>This acts as the single source of truth for all AI prompts in StudyGen,
 * mirroring the pattern used for environment variables in {@code .env} /
 * {@code application.properties}.</p>
 *
 * <p>All prompts are declared in {@code src/main/resources/prompts.properties}.
 * Services inject this bean and call the relevant method to obtain a
 * ready-to-use, runtime-formatted prompt string.</p>
 *
 * <pre>
 *   // .env analogy:
 *   @Value("${GOOGLE_GENAI_API_KEY}") private String apiKey;
 *
 *   // Prompt analogy:
 *   private final PromptRegistry prompts;
 *   ...
 *   String prompt = prompts.assessmentDiagnosticQuiz(topic, prerequisites);
 * </pre>
 */
@Component
@PropertySource("classpath:prompts.properties")
public class PromptRegistry {

    // -------------------------------------------------------------------------
    // 1. ASSESSMENT SERVICE
    // -------------------------------------------------------------------------

    /** Diagnostic quiz prompt. Placeholders: topic, claimedPrerequisites. */
    @Value("${prompts.assessment.diagnostic-quiz}")
    private String assessmentDiagnosticQuiz;

    /** Module mastery quiz prompt. Placeholders: moduleTitle, moduleDescription. */
    @Value("${prompts.assessment.module-quiz}")
    private String assessmentModuleQuiz;

    // -------------------------------------------------------------------------
    // 2. REMEDIATION SERVICE
    // -------------------------------------------------------------------------

    /** Gap-analysis prompt after a failed assessment. Placeholders: assessmentTitle, score (int), missedQuestionsSummary. */
    @Value("${prompts.remediation.gap-analysis}")
    private String remediationGapAnalysis;

    // -------------------------------------------------------------------------
    // 3. STUDY GUIDE SERVICE
    // -------------------------------------------------------------------------

    /** Study guide generation prompt. Placeholders: moduleTitle, moduleDescription, conceptContentBlock. */
    @Value("${prompts.study-guide.generate}")
    private String studyGuideGenerate;

    // -------------------------------------------------------------------------
    // 4. STUDY ASSISTANT SERVICE
    // -------------------------------------------------------------------------

    /** Tutor reply prompt with module + RAG context. Placeholders: context, studentQuestion. */
    @Value("${prompts.study-assistant.tutor-reply}")
    private String studyAssistantTutorReply;

    // -------------------------------------------------------------------------
    // 5. CHAT SESSION SERVICE
    // -------------------------------------------------------------------------

    /** Persistent chat tutor reply prompt. Placeholders: contextBlock (or ""), studentQuestion. */
    @Value("${prompts.chat-session.tutor-reply}")
    private String chatSessionTutorReply;

    // -------------------------------------------------------------------------
    // 6. ECOSYSTEM DISCOVERY SERVICE
    // -------------------------------------------------------------------------

    /** Resource discovery prompt. Placeholders: topic. */
    @Value("${prompts.ecosystem.discover}")
    private String ecosystemDiscover;

    // -------------------------------------------------------------------------
    // 7. CAREER MATCHING SERVICE
    // -------------------------------------------------------------------------

    /** Job skill extraction prompt. Placeholders: jobTitle, jobDescription. */
    @Value("${prompts.career.extract-skills}")
    private String careerExtractSkills;

    // =========================================================================
    // Public API — formatted prompt builders
    // Each method mirrors String.formatted() used in the original services.
    // =========================================================================

    /**
     * Builds the diagnostic quiz prompt.
     *
     * @param topic               the subject the student is being assessed on
     * @param claimedPrerequisites what the student claims to already know
     * @return formatted prompt string ready to send to the AI
     */
    public String assessmentDiagnosticQuiz(String topic, String claimedPrerequisites) {
        return assessmentDiagnosticQuiz.formatted(topic, claimedPrerequisites);
    }

    /**
     * Builds the module mastery quiz prompt.
     *
     * @param moduleTitle       the module's display title
     * @param moduleDescription short description of the module
     * @return formatted prompt string
     */
    public String assessmentModuleQuiz(String moduleTitle, String moduleDescription) {
        return assessmentModuleQuiz.formatted(moduleTitle, moduleDescription);
    }

    /**
     * Builds the remediation / gap-analysis prompt.
     *
     * @param assessmentTitle        title of the failed assessment
     * @param score                  the student's score as a percentage (e.g. 60)
     * @param missedQuestionsSummary bullet-list of missed questions and their explanations
     * @return formatted prompt string
     */
    public String remediationGapAnalysis(String assessmentTitle, int score, String missedQuestionsSummary) {
        return remediationGapAnalysis.formatted(assessmentTitle, score, missedQuestionsSummary);
    }

    /**
     * Builds the study guide generation prompt.
     *
     * @param moduleTitle       display title of the module
     * @param moduleDescription module's description text
     * @param conceptContent    concatenated concept titles + body text
     * @return formatted prompt string
     */
    public String studyGuideGenerate(String moduleTitle, String moduleDescription, String conceptContent) {
        return studyGuideGenerate.formatted(moduleTitle, moduleDescription, conceptContent);
    }

    /**
     * Builds the study-assistant tutor reply prompt.
     *
     * @param context         module context + uploaded RAG chunks (may be empty string)
     * @param studentQuestion the student's raw question text
     * @return formatted prompt string
     */
    public String studyAssistantTutorReply(String context, String studentQuestion) {
        return studyAssistantTutorReply.formatted(context, studentQuestion);
    }

    /**
     * Builds the chat-session tutor reply prompt.
     *
     * @param contextBlock    optional context block — pass {@code ""} if none
     * @param studentQuestion the student's raw question text
     * @return formatted prompt string
     */
    public String chatSessionTutorReply(String contextBlock, String studentQuestion) {
        return chatSessionTutorReply.formatted(
                contextBlock.isBlank() ? "" : "Context:\n" + contextBlock,
                studentQuestion
        );
    }

    /**
     * Builds the ecosystem discovery prompt.
     *
     * @param topic the technology / subject area to discover resources for
     * @return formatted prompt string
     */
    public String ecosystemDiscover(String topic) {
        return ecosystemDiscover.formatted(topic);
    }

    /**
     * Builds the career skill-extraction prompt.
     *
     * @param jobTitle       the job title from the listing
     * @param jobDescription the raw job description text
     * @return formatted prompt string
     */
    public String careerExtractSkills(String jobTitle, String jobDescription) {
        return careerExtractSkills.formatted(jobTitle, jobDescription);
    }
}
