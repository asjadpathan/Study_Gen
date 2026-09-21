# Detailed Overview: AI Features and Roadmap Generation in StudyGen

This document provides a comprehensive, in-depth analysis of the AI architecture, capabilities, and the specific implementations within the **StudyGen** project. The platform is fundamentally driven by Google's **Gemini 2.0 Flash** generative AI model, orchestrated through the **Google Genkit** framework.

---

## 1. AI Engine & Architecture Foundation

StudyGen transitions away from being a static content repository to becoming a dynamic intelligence platform.

### Genkit Configuration
The system connects to Gemini 2.0 Flash using Genkit server actions (`'use server'`).
- **Model:** `googleai/gemini-2.0-flash`
- **Location:** `src/ai/genkit.ts`
- **Architecture Pattern:** Every AI flow uses a strictly typed Zod schema for inputs and outputs. This guarantees structured JSON responses from the AI, preventing prompt injection or broken formatting issues on the client-side. The flows are defined as server actions, which Next.js securely calls from the client, keeping all API keys securely on the server.

There are **8 distinct AI Flows** built into StudyGen.

---

## 2. Roadmap Generation System (End-to-End)

The Roadmap Generation System is the core offering of the application. It maps a user's intent to a deeply customized learning curriculum.

### Step 2.1: Data Collection & Skill Assessment
When a user navigates to `/roadmap/create`, they are presented with a multi-step wizard.
1. **Initial Input:** User provides their learning **goals** (e.g., "Master React"), current **expertise**, **available study time**, and optional specific **topics** to include/exclude.
2. **Dynamic Skill Assessment (`generateQuizAndExplanation` flow):** 
   - Based on the user's input, the system generates **3 assessment questions** in parallel.
   - These are dynamically prompted to be: (1) Foundational, (2) Advanced, and (3) Practical/Application-based.
   - The user takes this mini-quiz. The answers (both correct and incorrect) are captured.

### Step 2.2: Roadmap Generation (`generatePersonalizedRoadmap` flow)
The captured quiz results are appended to the user's `expertise` field, acting as a feedback loop. The AI now knows not just what the user claims to know, but what they have *demonstrated* they know.
- **Input Schema:** `goals`, `expertise` (with quiz summary), `availableStudyTime`, `specificTopics`.
- **AI Task:** The AI acts as an expert learning generator, utilizing the assessment to identify knowledge gaps.
- **Output Schema:** A structured JSON object containing an array of modules. Each module has a `title` and an array of granular `concepts`.
- **Implementation File:** `src/ai/flows/personalized-roadmap.ts`

### Step 2.3: Storage and State Management
Once generated, the roadmap is saved to Firebase Cloud Firestore at `users/{uid}/roadmaps/{roadmapId}`.
This document stores:
- Original form inputs.
- The generated roadmap structure (modules and concepts).
- A `completedConcepts` array to track progress.
- Caching objects for explanations and resources (see Retrieval System below).

---

## 3. The Retrieval and Caching System (Explanation Engine)

To prevent redundant API calls, reduce costs, and provide instantaneous feedback, StudyGen uses a highly optimized **Retrieval and Caching System** via Firestore.

### The Concept Viewer (`getConceptExplanation` flow)
When a user clicks on a specific concept within their generated roadmap, the following logic executes:

1. **Cache Lookup:** The client queries the active Firestore roadmap document (`roadmap.explanations[conceptName]`).
2. **Cache Hit:** If the explanation exists in the database, it is retrieved and rendered instantly as HTML. No AI call is made.
3. **Cache Miss (Generation):**
   - If the explanation does not exist, the `getConceptExplanation` flow is triggered.
   - **Input:** Concept name, broader topic (roadmap title), and the user's expertise level.
   - **AI Task:** Generates a personalized Markdown explanation containing definitions, core theory, and relevant practical examples.
   - **Retrieval & Storage:** The generated explanation is displayed to the user and simultaneously written back to the Firestore document under `explanations.{conceptName}`.
4. **Result:** Over time, the roadmap document self-assembles into a fully comprehensive, pre-generated, personalized textbook.

---

## 4. Quizzing and Active Feedback Loop

Active recall is facilitated by a multi-purpose AI flow (`active-feedback.ts`) that handles both question generation and intelligent error correction.

### Dual-Mode AI Logic (`generateQuizAndExplanation`)
This single flow handles three different use cases by leveraging Handlebars conditionals in the prompt:

1. **Concept Quizzes:** After reading an explanation, users click "Test Your Knowledge." The AI generates a 4-option multiple-choice question on the spot.
2. **Intelligent Wrong-Answer Feedback:**
   - If a user answers incorrectly, the exact same flow is called again, but this time in **Feedback Mode**.
   - **Input provided:** The question, the `userAnswer`, and the `correctAnswer`.
   - **AI Task:** Generates a simplified explanation detailing exactly *why* the right answer is correct, *why* the user's answer is wrong, and corrects the underlying misconception.
3. **Review Quizzes (`generateReviewQuiz` flow):** 
   - A dedicated flow (`review-quiz.ts`) that takes an array of all `completedConcepts`.
   - It generates a comprehensive 10+ question review quiz spanning all learned materials, acting as spaced repetition.

---

## 5. Material Processing System (Multimodal AI)

StudyGen allows users to upload their own files (PDF, DOCX, TXT, PNG, JPG).

- **Implementation File:** `src/ai/flows/process-material.ts`
- **Mechanism:** Files are converted to Base64 Data URIs on the client and sent to the server.
- **Multimodal AI:** Genkit uses the `{{media url=fileDataUri}}` helper to pass the file directly to Gemini 2.0 Flash.
- **Output:** In a single generation pass, the AI extracts the text and returns:
  1. A concise **Summary**.
  2. 5-10 interactive **Quiz Questions**.
  3. 5-10 **Flashcards** (terms and definitions).
- **UX Integration:** Flashcards are rendered using pure CSS 3D transforms (`rotateY(180deg)`), and quizzes integrate the same Active Feedback loop for incorrect answers.

---

## 6. Resource Discovery and Global Search

### AI-Curated Resource Discovery (`findResources` flow)
For every generated roadmap, the AI automatically curates real-world resources.
- **Trigger:** Auto-fetches in the background when a roadmap detail page loads (if not already cached).
- **Output Schema:** 4 categories of resources (Learning Resources, Competitions, News/Journals, Job Opportunities).
- **Caching:** Saved immediately to `roadmap.resources` in Firestore so it only costs one AI call per roadmap.

### Global Topic Search (`searchTopic` flow)
Available via the top navigation bar.
- **Input:** Any user search query.
- **Output:** A customized markdown explanation, a catchy quiz title to redirect the user to the study zone, and 3-5 external resource links.

---

## 7. Future Capability: Adaptive Personalization

Defined in `adaptive-personalization.ts`, this flow is designed to observe a user's performance across quizzes and dynamically alter the difficulty of future generated content. While fully defined as an AI schema in the project's source, it acts as the foundation for the next iteration of the product.

---

## Summary

The StudyGen architecture perfectly balances **Generative AI** with **Traditional Caching**. 
- It uses AI for what AI is best at: personalizing curriculum, assessing skill gaps via quizzes, and providing customized explanations for wrong answers.
- It uses Firestore for what databases are best at: storing those expensive AI generations (explanations, resources) so they can be retrieved instantly at zero additional cost on subsequent visits.
