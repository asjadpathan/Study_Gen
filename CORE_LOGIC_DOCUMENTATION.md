# StudyGen — Complete Core Logic & Technical Documentation

> **Version**: 1.0  
> **Last Updated**: June 29, 2026  
> **Project**: StudyGen (StudyGenius) — AI-Powered Learning Platform

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Architecture Overview](#2-architecture-overview)
3. [Technology Stack Deep Dive](#3-technology-stack-deep-dive)
4. [Project Structure](#4-project-structure)
5. [Configuration & Environment](#5-configuration--environment)
6. [Firebase Integration](#6-firebase-integration)
   - 6.1 [Firebase Initialization](#61-firebase-initialization)
   - 6.2 [Authentication System](#62-authentication-system)
   - 6.3 [Firestore Data Model](#63-firestore-data-model)
   - 6.4 [Security Rules](#64-security-rules)
7. [AI Engine — Google Genkit](#7-ai-engine--google-genkit)
   - 7.1 [Genkit Configuration](#71-genkit-configuration)
   - 7.2 [Flow Architecture Pattern](#72-flow-architecture-pattern)
   - 7.3 [Flow 1: Personalized Roadmap Generation](#73-flow-1-personalized-roadmap-generation)
   - 7.4 [Flow 2: Concept Explanation](#74-flow-2-concept-explanation)
   - 7.5 [Flow 3: Active Feedback (Quiz & Explanation)](#75-flow-3-active-feedback-quiz--explanation)
   - 7.6 [Flow 4: Review Quiz](#76-flow-4-review-quiz)
   - 7.7 [Flow 5: Material Processing](#77-flow-5-material-processing)
   - 7.8 [Flow 6: Resource Discovery](#78-flow-6-resource-discovery)
   - 7.9 [Flow 7: Topic Search](#79-flow-7-topic-search)
   - 7.10 [Flow 8: Adaptive Personalization](#710-flow-8-adaptive-personalization)
8. [Page-by-Page Logic Breakdown](#8-page-by-page-logic-breakdown)
   - 8.1 [Root Page — Auth Gate](#81-root-page--auth-gate)
   - 8.2 [Login Page](#82-login-page)
   - 8.3 [Signup Page](#83-signup-page)
   - 8.4 [Dashboard](#84-dashboard)
   - 8.5 [Roadmap List Page](#85-roadmap-list-page)
   - 8.6 [Roadmap Creation Wizard](#86-roadmap-creation-wizard)
   - 8.7 [Roadmap Detail Viewer](#87-roadmap-detail-viewer)
   - 8.8 [Upload & Material Processing Page](#88-upload--material-processing-page)
   - 8.9 [Study Zone (Community)](#89-study-zone-community)
   - 8.10 [Saved Resources Page](#810-saved-resources-page)
   - 8.11 [Search Page](#811-search-page)
9. [Shared Components](#9-shared-components)
   - 9.1 [Main Header](#91-main-header)
   - 9.2 [Discussion Channel](#92-discussion-channel)
   - 9.3 [Search Results](#93-search-results)
   - 9.4 [Landing Page](#94-landing-page)
   - 9.5 [Page Transition Wrapper](#95-page-transition-wrapper)
   - 9.6 [Logo Component](#96-logo-component)
10. [Design System](#10-design-system)
11. [Data Flow Diagrams](#11-data-flow-diagrams)
12. [Utility Functions](#12-utility-functions)
13. [Custom Hooks](#13-custom-hooks)
14. [Deployment & Hosting](#14-deployment--hosting)

---

## 1. Executive Summary

**StudyGen** is a full-stack, AI-powered learning platform that uses **generative AI** (Google Gemini 2.0 Flash via Genkit) to create personalized learning experiences. The platform allows users to:

- Create AI-generated personalized learning roadmaps based on their goals, expertise, and time constraints
- Receive AI-generated explanations for individual concepts within a roadmap
- Take AI-generated quizzes to test understanding, with intelligent feedback on incorrect answers
- Upload study materials (PDF, DOCX, TXT, images) and have AI convert them into summaries, quizzes, and flashcards
- Discover curated learning resources, competitions, news, and job opportunities
- Participate in real-time community discussions
- Track learning progress with dashboards, streaks, and metrics
- Bookmark and manage saved resources

The platform is built as a **Next.js 15 App Router** application with **Firebase** for authentication, database (Firestore), and analytics. All AI features use **server actions** (`'use server'`) to keep API keys server-side.

---

## 2. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser)                             │
│                                                                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────────┐   │
│  │ Landing Page │  │  Auth Pages  │  │  Authenticated App       │   │
│  │  (page.tsx)  │  │ login/signup │  │  (main) route group      │   │
│  │              │  │              │  │  ┌────────────────────┐  │   │
│  │ Checks auth  │  │ Firebase Auth│  │  │ Dashboard          │  │   │
│  │ → redirect   │  │ Email/Pass   │  │  │ Roadmap CRUD       │  │   │
│  │   or show    │  │ + Firestore  │  │  │ Upload & Process   │  │   │
│  │   landing    │  │   user doc   │  │  │ Study Zone (Chat)  │  │   │
│  └──────────────┘  └──────────────┘  │  │ Saved Resources    │  │   │ 
│                                      │  │ Global Search      │  │   │
│                                      │  └────────────────────┘  │   │
│                                      └──────────────────────────┘   │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                    Server Actions ('use server')
                                │
                    ┌───────────▼───────────┐
                    │   Next.js Server      │
                    │                       │
                    │   8 Genkit AI Flows   │
                    │   ┌──────────────┐    │
                    │   │ Gemini 2.0   │    │
                    │   │ Flash Model  │    │
                    │   └──────────────┘    │
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │   Firebase Services   │
                    │                       │
                    │  • Authentication     │
                    │  • Cloud Firestore    │
                    │  • Analytics          │
                    └───────────────────────┘
```

**Key Architectural Decisions:**

1. **All AI flows run server-side** via `'use server'` directive — this ensures API keys never reach the client and allows server-side structured output parsing.
2. **Real-time data** — The app uses Firestore `onSnapshot` listeners extensively for real-time updates (dashboard stats, roadmap progress, chat messages, saved resources).
3. **Client-side routing protection** — Auth state is checked on every page via `onAuthStateChanged` listeners in `useEffect`. There is no middleware-based protection; the (main) layout does not enforce auth, but individual pages redirect or show errors if no user is present.
4. **Caching strategy** — AI-generated explanations are cached in Firestore (stored on the roadmap document) so they don't need to be regenerated. Resources are also fetched once and persisted.

---

## 3. Technology Stack Deep Dive

| Technology | Version | Role | Why It's Used |
|---|---|---|---|
| **Next.js** | 15.3.3 | Framework (App Router) | Server components, server actions, file-based routing, API-less architecture |
| **React** | 18.3.1 | UI Library | Component-based UI with hooks |
| **TypeScript** | ^5 | Language | Type safety across the entire codebase |
| **Google Genkit** | ^1.14.1 | AI Framework | Structured AI flow orchestration with schema validation |
| **@genkit-ai/googleai** | ^1.14.1 | AI Plugin | Connects Genkit to Google's Gemini models |
| **Gemini 2.0 Flash** | — | AI Model | Fast, cost-effective generative AI for all 8 flows |
| **Firebase** | ^11.9.1 | Backend-as-a-Service | Auth (Email/Password), Firestore (NoSQL DB), Analytics |
| **Tailwind CSS** | ^3.4.1 | Styling | Utility-first CSS with custom design tokens |
| **ShadCN UI** | — | Component Library | 37 pre-built accessible components (Radix UI primitives) |
| **Framer Motion** | ^11.5.7 | Animations | Page transitions, staggered reveals, micro-animations |
| **React Hook Form** | ^7.54.2 | Form Management | Performant forms with minimal re-renders |
| **Zod** | ^3.24.2 | Schema Validation | Form validation + AI flow input/output schemas |
| **Recharts** | ^2.15.1 | Charts | Dashboard progress bar chart |
| **react-dropzone** | ^14.2.3 | File Upload | Drag-and-drop file upload UI |
| **date-fns** | ^3.6.0 | Date Utilities | Relative time formatting in chat messages |
| **Lucide React** | ^0.475.0 | Icons | Consistent icon set throughout the UI |
| **Vercel Speed Insights** | ^1.2.0 | Performance Monitoring | Real user performance metrics |

---

## 4. Project Structure

```
studygen/
│
├── src/
│   ├── ai/                              # AI LAYER
│   │   ├── genkit.ts                    # Genkit instance initialization
│   │   ├── dev.ts                       # Dev server entry point (imports all flows)
│   │   ├── flows/                       # 8 AI flow definitions
│   │   │   ├── personalized-roadmap.ts  # Roadmap generation
│   │   │   ├── get-concept-explanation.ts  # Concept explanations
│   │   │   ├── active-feedback.ts       # Quiz generation + wrong answer feedback
│   │   │   ├── review-quiz.ts           # Multi-topic review quiz generation
│   │   │   ├── process-material.ts      # Document → summary + quiz + flashcards
│   │   │   ├── find-resources.ts        # Resource/competition/news/job discovery
│   │   │   ├── search-topic.ts          # Global topic search
│   │   │   └── adaptive-personalization.ts  # Content adaptation based on performance
│   │   └── schemas/
│   │       └── review-quiz.ts           # Shared Zod schemas for review quiz
│   │
│   ├── app/                             # NEXT.JS APP ROUTER
│   │   ├── layout.tsx                   # Root layout (HTML, fonts, Toaster, ChunkLoadError handler)
│   │   ├── globals.css                  # Design tokens (HSL CSS variables), light/dark themes
│   │   ├── page.tsx                     # Root page (auth gate → landing or dashboard redirect)
│   │   ├── favicon.ico
│   │   ├── login/page.tsx               # Login page with email/password
│   │   ├── signup/page.tsx              # Signup page with email/password
│   │   └── (main)/                      # AUTHENTICATED ROUTE GROUP
│   │       ├── layout.tsx               # MainHeader + PageTransitionWrapper
│   │       ├── dashboard/page.tsx       # Dashboard with metrics, chart, upcoming lessons
│   │       ├── roadmap/
│   │       │   ├── page.tsx             # Roadmap list (grid view with progress)
│   │       │   ├── create/page.tsx      # Multi-step roadmap creation wizard
│   │       │   └── [id]/page.tsx        # Roadmap detail viewer (the largest, most complex page)
│   │       ├── study/page.tsx           # Community study zone with discussion channel
│   │       ├── upload/page.tsx          # Material upload + AI processing results
│   │       ├── saved-resources/page.tsx # Bookmarked resources library
│   │       └── search/page.tsx          # Search results (delegates to SearchResults component)
│   │
│   ├── components/                      # SHARED COMPONENTS
│   │   ├── landing-page.tsx             # Full marketing landing page (33KB)
│   │   ├── main-header.tsx              # Navigation header with search + user menu
│   │   ├── discussion-channel.tsx       # Real-time Firestore chat component
│   │   ├── search-results.tsx           # AI-powered search results display
│   │   ├── page-transition-wrapper.tsx  # Framer Motion page transitions
│   │   ├── logo.tsx                     # StudyGen logo/branding
│   │   └── ui/                          # 37 ShadCN UI primitives
│   │
│   ├── hooks/                           # CUSTOM HOOKS
│   │   ├── use-mobile.tsx               # Mobile breakpoint detection (768px)
│   │   └── use-toast.ts                 # Toast notification system (max 1 toast)
│   │
│   └── lib/                             # UTILITIES
│       ├── firebase.ts                  # Firebase app/auth/db/analytics initialization
│       └── utils.ts                     # cn() helper (clsx + tailwind-merge)
│
├── docs/
│   └── blueprint.md                     # Original project blueprint/requirements
│
├── package.json                         # Dependencies and scripts
├── next.config.ts                       # Next.js configuration
├── tailwind.config.ts                   # Tailwind CSS configuration with custom fonts
├── tsconfig.json                        # TypeScript configuration
├── apphosting.yaml                      # Firebase App Hosting configuration
└── components.json                      # ShadCN UI configuration
```

---

## 5. Configuration & Environment

### 5.1 Environment Variables

The app requires the following environment variables in a `.env` file:

```env
NEXT_PUBLIC_FIREBASE_API_KEY=...
NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN=...
NEXT_PUBLIC_FIREBASE_PROJECT_ID=...
NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET=...
NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=...
NEXT_PUBLIC_FIREBASE_APP_ID=...
NEXT_PUBLIC_FIREBASE_MEASUREMENT_ID=...
```

All Firebase variables are prefixed with `NEXT_PUBLIC_` because they need to be accessible on the client side (Firebase client SDK requires them in the browser). This is safe because Firebase security is enforced through Firestore Security Rules and Firebase Auth, not through secret API keys.

The **Genkit/Gemini API key** is expected to be available via the `GOOGLE_GENAI_API_KEY` environment variable (used by the `@genkit-ai/googleai` plugin internally). This is NOT prefixed with `NEXT_PUBLIC_` as it only runs server-side.

### 5.2 NPM Scripts

| Script | Command | Purpose |
|---|---|---|
| `dev` | `next dev -p 9002` | Start development server on port 9002 |
| `build` | `next build` | Create production build |
| `start` | `next start` | Start production server |
| `lint` | `next lint` | Run ESLint |
| `typecheck` | `tsc --noEmit` | TypeScript type checking |
| `genkit:dev` | `genkit start -- tsx src/ai/dev.ts` | Start Genkit dev UI for testing flows |
| `genkit:watch` | `genkit start -- tsx --watch src/ai/dev.ts` | Start Genkit dev UI with hot reload |

### 5.3 Fonts

The app uses two Google Fonts loaded via `<link>` tags in the root layout:

- **Inter** (400, 500, 600, 700) — Body text (`font-body` in Tailwind config)
- **Space Grotesk** (400, 500, 700) — Headlines (`font-headline` in Tailwind config)

---

## 6. Firebase Integration

### 6.1 Firebase Initialization

**File**: `src/lib/firebase.ts`

```typescript
const app = getApps().length ? getApp() : initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);
```

**Key Logic:**
- Uses `getApps().length ? getApp() : initializeApp(firebaseConfig)` to prevent re-initialization during Next.js hot reloads (HMR). Without this guard, calling `initializeApp()` multiple times would throw an error.
- Exports `app`, `auth`, `db`, and `analytics` for use throughout the application.
- Analytics initialization is wrapped in `typeof window !== 'undefined'` to prevent server-side execution (analytics only works in the browser).

### 6.2 Authentication System

The app uses **Firebase Authentication with Email/Password** provider.

#### Login Flow (`src/app/login/page.tsx`):

1. **Auth Check on Mount**: `onAuthStateChanged` listener checks if user is already logged in → if yes, redirects to `/dashboard`.
2. **Form Validation**: Uses `react-hook-form` with Zod schema requiring valid email and minimum 6-character password.
3. **Sign-In**: Calls `signInWithEmailAndPassword(auth, email, password)`.
4. **Firestore User Document Check**: After successful login, checks if a user document exists at `users/{uid}`. If NOT (edge case — user exists in Auth but not Firestore), creates one with default values:
   ```typescript
   {
     email: user.email,
     createdAt: serverTimestamp(),
     studyStreak: { count: 0, lastUpdate: '' },
     skillsMastered: 0,
     timeStudied: 0,
   }
   ```
5. **Redirect**: The `onAuthStateChanged` listener (from step 1) detects the auth state change and triggers the redirect to `/dashboard`.

#### Signup Flow (`src/app/signup/page.tsx`):

1. **Auth Check**: Same as login — if already authenticated, redirect to `/dashboard`.
2. **User Creation**: Calls `createUserWithEmailAndPassword(auth, email, password)`.
3. **Firestore User Document**: Always creates a new user document (with existence check first via `getDoc`) with the same default structure as the login flow.
4. **Success Toast**: Shows "Account Created" toast notification.
5. **Redirect**: Same `onAuthStateChanged`-driven redirect.

#### Logout Flow (`src/components/main-header.tsx`):

```typescript
const handleLogout = async () => {
  await signOut(auth);
  router.push("/login");
};
```

Simple sign-out followed by redirect to login page.

### 6.3 Firestore Data Model

#### Collection: `users/{userId}`

The root user document stores profile and aggregate metrics.

```typescript
{
  email: string,                    // User's email address
  createdAt: Timestamp,             // Firebase server timestamp
  studyStreak: {
    count: number,                  // Current streak in days
    lastUpdate: string              // ISO date of last streak update
  },
  skillsMastered: number,           // Total skills mastered count
  timeStudied: number               // Total time studied in minutes
}
```

#### Subcollection: `users/{userId}/roadmaps/{roadmapId}`

Each roadmap document stores the full AI-generated roadmap along with user progress.

```typescript
{
  // User Input (from creation form)
  goals: string,                     // "Pass the AP Calculus Exam"
  expertise: string,                 // "Some experience with derivatives"
  availableStudyTime: string,        // "5 hours a week for 3 months"
  specificTopics?: string,           // "Focus on integration, skip related rates"
  
  // AI Assessment Results
  quizSummary: string,               // Formatted quiz Q&A from skill assessment
  
  // AI-Generated Roadmap (from personalizedRoadmapFlow)
  roadmap: Array<{
    title: string,                   // "Module 1: Foundations of Calculus"
    concepts: string[]               // ["Limits", "Continuity", "Derivatives"]
  }>,
  
  // User Progress
  completedConcepts: string[],       // ["Limits", "Continuity"] — array of concept names
  
  // Cached AI Content
  explanations?: Record<string, {    // Cached concept explanations (keyed by concept name)
    explanation: string              // Markdown-formatted explanation
  }>,
  
  resources?: {                      // Cached resource discovery results
    learningResources: Array<{ title, description, url }>,
    competitions: Array<{ title, description, url }>,
    news: Array<{ title, description, url }>,
    jobs: Array<{ title, description, url }>
  },
  
  // Metadata
  createdAt: Timestamp               // Firebase server timestamp
}
```

#### Subcollection: `users/{userId}/savedResources/{resourceId}`

Bookmarked resources from the Resources page.

```typescript
{
  title: string,                     // Resource title
  description: string,               // Short description
  url: string,                       // External URL
  type: 'learning' | 'competition' | 'news',  // Resource category
  savedAt: Timestamp                 // When the user saved it
}
```

#### Collection: `discussionChannels/{channelId}`

Top-level collection for community discussion channels.

```typescript
{
  name: string,                      // "General Chat"
  description: string                // "Talk about anything study-related."
}
```

#### Subcollection: `discussionChannels/{channelId}/messages/{messageId}`

Individual messages within a channel.

```typescript
{
  text: string,                      // Message content
  createdAt: Timestamp,              // Firebase server timestamp
  userId: string,                    // Auth UID of the sender
  userEmail: string                  // Email of the sender (for display)
}
```

### 6.4 Security Rules

The Firestore security rules enforce the following access patterns:

| Path | Read | Write | Delete |
|---|---|---|---|
| `users/{userId}` | Only by owner (`auth.uid == userId`) | Only by owner | Only by owner |
| `users/{userId}/roadmaps/{roadmapId}` | Only by owner | Only by owner | Only by owner |
| `users/{userId}/savedResources/{resourceId}` | Only by owner | Only by owner | Only by owner |
| `discussionChannels/{channelId}` | Any authenticated user | Any authenticated user | — |
| `discussionChannels/.../messages/{messageId}` | Any authenticated user | Creator only (validates `userId` and required fields) | Denied (`false`) |

**Key Security Enforcement on Messages:**
- The `create` rule validates that `request.auth.uid == request.resource.data.userId` (users can only post as themselves)
- Required fields `text` and `userEmail` must be present
- No user can update or delete messages once posted

---

## 7. AI Engine — Google Genkit

### 7.1 Genkit Configuration

**File**: `src/ai/genkit.ts`

```typescript
import { genkit } from 'genkit';
import { googleAI } from '@genkit-ai/googleai';

export const ai = genkit({
  plugins: [googleAI()],
  model: 'googleai/gemini-2.0-flash',
});
```

- **Model**: `googleai/gemini-2.0-flash` — Google's fast, cost-effective generative AI model. Chosen for quick response times across all 8 flows.
- **Plugin**: `@genkit-ai/googleai` — Connects Genkit to Google AI services.
- The `ai` instance is exported and used by all flow definitions.

### 7.2 Flow Architecture Pattern

Every AI flow in StudyGen follows the same three-layer pattern:

```
  ┌──────────────────────────────┐
  │  1. Export Function          │  ← Called by page components (server action)
  │  async function doThing()    │     Thin wrapper that calls the flow
  │  { return flow(input); }     │
  ├──────────────────────────────┤
  │  2. Prompt Definition        │  ← Defines the LLM prompt template
  │  ai.definePrompt({           │     Uses Handlebars syntax for interpolation
  │    name, input, output,      │     Input/Output validated by Zod schemas
  │    prompt: `...`             │
  │  })                          │
  ├──────────────────────────────┤
  │  3. Flow Definition          │  ← Orchestrates prompt execution
  │  ai.defineFlow({             │     Named, typed, validated
  │    name, inputSchema,        │     Calls the prompt and returns output
  │    outputSchema              │
  │  }, async (input) => {       │
  │    const { output } = await  │
  │      prompt(input);          │
  │    return output!;           │
  │  })                          │
  └──────────────────────────────┘
```

All flows use `'use server'` directive, meaning they run exclusively on the Next.js server. Client components call these functions directly — Next.js automatically handles the serialization/deserialization of inputs and outputs across the network boundary.

### 7.3 Flow 1: Personalized Roadmap Generation

**File**: `src/ai/flows/personalized-roadmap.ts`  
**Function**: `generatePersonalizedRoadmap(input)`  
**Called by**: `src/app/(main)/roadmap/create/page.tsx`

#### Input Schema

| Field | Type | Required | Description |
|---|---|---|---|
| `goals` | `string` | Yes | Learning goals (e.g., "Pass the AP Calculus Exam") |
| `expertise` | `string` | Yes | Current expertise + optional skill assessment results |
| `availableStudyTime` | `string` | Yes | Time commitment (e.g., "1 hour per day for 3 months") |
| `learningStyle` | `string[]` | No | Preferred styles: "Visual", "Auditory", "Kinesthetic" |
| `preferredResourceTypes` | `string[]` | No | Preferred resources: "Videos", "Articles", etc. |
| `specificTopics` | `string` | No | Topics to include or exclude |

#### Output Schema

```typescript
{
  roadmap: Array<{
    title: string,       // "Module 1: Foundations"
    concepts: string[]   // ["Limits", "Continuity", "Derivative Definition"]
  }>
}
```

#### Prompt Logic

The prompt instructs the AI to:
1. Act as an "expert learning roadmap generator"
2. Use skill assessment results (if present in `expertise`) to identify knowledge gaps
3. Consider learning style and resource preferences when structuring the plan
4. Create a "highly detailed" roadmap "broken down into weekly modules"
5. Each module must contain "specific, granular concepts to learn"
6. Return structured JSON matching the output schema

#### How It's Used

The roadmap creation wizard (see Section 8.6) calls this flow after the user completes a skill assessment quiz. The quiz results are appended to the `expertise` field, giving the AI context about what the user already knows and where they struggle.

### 7.4 Flow 2: Concept Explanation

**File**: `src/ai/flows/get-concept-explanation.ts`  
**Function**: `getConceptExplanation(input)`  
**Called by**: `src/app/(main)/roadmap/[id]/page.tsx`

#### Input Schema

| Field | Type | Required | Description |
|---|---|---|---|
| `concept` | `string` | Yes | The specific concept to explain |
| `topic` | `string` | Yes | The broader learning goal (roadmap title) |
| `expertise` | `string` | Yes | User's stated expertise level |

#### Output Schema

```typescript
{
  explanation: string  // Detailed explanation in Markdown format
}
```

#### Prompt Logic

The AI acts as an "expert educator" and generates:
- Definitions of the concept
- Core theory
- Practical examples/applications relevant to the user's learning goals
- Content tailored to the user's stated expertise level
- Full response formatted in Markdown

#### Caching Behavior

When a user clicks on a concept in the roadmap viewer:
1. **Check cache**: The app first checks `roadmap.explanations[concept]` in the Firestore document
2. **Cache hit**: If found, the cached explanation is used immediately (no AI call)
3. **Cache miss**: The AI flow is called, the result is displayed, AND saved back to Firestore under `explanations.{conceptName}` for future use
4. This means each concept explanation is generated **at most once** per roadmap

### 7.5 Flow 3: Active Feedback (Quiz & Explanation)

**File**: `src/ai/flows/active-feedback.ts`  
**Function**: `generateQuizAndExplanation(input)`  
**Called by**: Multiple pages (roadmap create, roadmap detail, upload)

This is the **most versatile flow** — used for three different purposes:

#### Purpose 1: Skill Assessment During Roadmap Creation
When creating a roadmap, this flow generates 3 assessment questions to gauge the user's level.

#### Purpose 2: Concept Quizzes in Roadmap Viewer
After reading a concept explanation, users can take a quiz. If they answer incorrectly, this flow is called again with the wrong answer context to generate a simplified explanation.

#### Purpose 3: Material Upload Quiz Feedback
When users answer quiz questions generated from uploaded materials incorrectly, this flow provides explanations.

#### Input Schema

| Field | Type | Required | Description |
|---|---|---|---|
| `topic` | `string` | Yes | The topic for quiz generation |
| `userAnswer` | `string` | No | The user's incorrect answer (for feedback mode) |
| `correctAnswer` | `string` | No | The correct answer (for feedback mode) |
| `context` | `string` | No | Additional context (e.g., "Question 1 of 3") |

#### Output Schema

```typescript
{
  question: string,      // The quiz question
  options: string[],     // 4 multiple-choice options
  correctAnswer: string, // The correct option
  explanation: string    // Simplified explanation of the answer
}
```

#### Dual-Mode Prompt Logic

The prompt uses Handlebars conditionals:
- **Generation Mode** (no `userAnswer`): Generates a new quiz question with 4 options
- **Feedback Mode** (with `userAnswer` + `correctAnswer`): Generates a simplified explanation of why the correct answer is correct and why the user's answer was wrong

### 7.6 Flow 4: Review Quiz

**File**: `src/ai/flows/review-quiz.ts`  
**Schema File**: `src/ai/schemas/review-quiz.ts`  
**Function**: `generateReviewQuiz(input)`  
**Called by**: `src/app/(main)/roadmap/[id]/page.tsx`

#### Input Schema

```typescript
{
  topics: string[]  // List of completed concept names
}
```

#### Output Schema

```typescript
{
  quiz: Array<{
    question: string,      // Quiz question
    options: string[],     // 4 options
    correctAnswer: string  // The correct option
  }>
}
```

#### Prompt Logic

- Generates a minimum of **10 multiple-choice questions** from the provided topics
- If fewer than 10 topics are provided, generates multiple questions per topic to reach the minimum
- Each question has exactly 4 options

#### Why the Schema is Separate

The `ReviewQuizInputSchema` and `ReviewQuizOutputSchema` are defined in a separate file (`src/ai/schemas/review-quiz.ts`) because the output types are also imported by the roadmap detail page component for TypeScript typing of the quiz state.

### 7.7 Flow 5: Material Processing

**File**: `src/ai/flows/process-material.ts`  
**Function**: `processMaterial(input)`  
**Called by**: `src/app/(main)/upload/page.tsx`

#### Input Schema

```typescript
{
  fileDataUri: string  // Base64 data URI: "data:<mimetype>;base64,<encoded_data>"
}
```

#### Output Schema

```typescript
{
  summary: string,                    // Concise summary of the document
  quiz: Array<{
    question: string,                 // Quiz question
    options: string[],                // Multiple-choice options
    correctAnswer: string             // The correct answer
  }>,
  flashcards: Array<{
    term: string,                     // Key term or concept
    definition: string               // Definition or explanation
  }>
}
```

#### Prompt Logic

The AI performs four tasks:
1. **Extract text** from the uploaded document (Genkit handles multimodal input via `{{media url=fileDataUri}}`)
2. **Generate a concise summary** of the content
3. **Create 5-10 multiple-choice quiz questions** with 4 options each
4. **Create 5-10 flashcards** with terms and definitions

#### File Handling

On the client side, the file is read using `FileReader.readAsDataURL()` which produces a base64 data URI. This string is sent to the server action. Genkit's `{{media url=fileDataUri}}` template helper processes the multimodal input.

**Supported file types**: PDF, DOCX, TXT, PNG, JPG/JPEG

### 7.8 Flow 6: Resource Discovery

**File**: `src/ai/flows/find-resources.ts`  
**Function**: `findResources(input)`  
**Called by**: `src/app/(main)/roadmap/[id]/page.tsx`

#### Input Schema

```typescript
{
  topic: string  // The roadmap goal (e.g., "Machine Learning")
}
```

#### Output Schema

```typescript
{
  learningResources: Array<{ title, description, url }>,  // Articles, courses, videos
  competitions: Array<{ title, description, url }>,       // Hackathons, challenges
  news: Array<{ title, description, url }>,               // Recent articles, journals
  jobs: Array<{ title, description, url }>                // Career opportunities
}
```

#### Prompt Logic

The AI acts as a "helpful assistant that curates resources" and finds 2-4 items per category. URLs should be valid and working.

#### Auto-Fetch & Cache Behavior

When a roadmap detail page loads:
1. **Check Firestore**: If `roadmap.resources` exists, use the cached data
2. **If missing**: Automatically call `findResources({ topic: roadmap.goals })` in the background
3. **Save to Firestore**: Store the results on the roadmap document for future visits

### 7.9 Flow 7: Topic Search

**File**: `src/ai/flows/search-topic.ts`  
**Function**: `searchTopic(input)`  
**Called by**: `src/components/search-results.tsx`

#### Input Schema

```typescript
{
  topic: string  // The search query
}
```

#### Output Schema

```typescript
{
  explanation: string,                // Detailed Markdown explanation
  quizTitle: string,                  // A catchy quiz title for the topic
  resources: Array<{
    title: string,
    description: string,
    url: string
  }>                                  // 3-5 external resources
}
```

#### Prompt Logic

The AI acts as an "expert educator and content curator" and provides:
1. A detailed, easy-to-understand explanation in Markdown format
2. A catchy quiz title
3. 3-5 external resources with titles, descriptions, and valid URLs

### 7.10 Flow 8: Adaptive Personalization

**File**: `src/ai/flows/adaptive-personalization.ts`  
**Function**: `adaptLearning(input)`  
**Status**: Defined but not actively called from any page component

#### Input Schema

| Field | Type | Description |
|---|---|---|
| `currentContent` | `string` | Content currently being shown to the user |
| `userPerformance` | `string` | Summary of strengths and weaknesses |
| `learningGoals` | `string` | Overall learning goals |

#### Output Schema

```typescript
{
  adaptedContent: string,            // Modified content for the user
  pacingRecommendation: string,      // Suggestions for study pacing
  remedialActions: string            // Specific actions for weak areas
}
```

> **Note**: This flow is defined and registered in the Genkit dev server but is not currently invoked from any page or component. It appears to be a planned feature for future implementation where the platform would dynamically adjust content difficulty and pacing based on user quiz performance.

---

## 8. Page-by-Page Logic Breakdown

### 8.1 Root Page — Auth Gate

**File**: `src/app/page.tsx`  
**Route**: `/`

**Logic Flow:**
1. Registers an `onAuthStateChanged` listener on mount
2. **While checking**: Shows a centered loading spinner
3. **If user IS authenticated**: Calls `router.push('/dashboard')` → user sees a brief spinner while redirecting
4. **If user is NOT authenticated**: Renders the `<LandingPage />` component (full marketing landing page)

**State:**
- `user: User | null` — Firebase auth user
- `loading: boolean` — Whether the auth check is in progress

### 8.2 Login Page

**File**: `src/app/login/page.tsx`  
**Route**: `/login`

**Full Logic:**

1. **Auth guard**: If already logged in, redirect to `/dashboard`
2. **Form**: Email + Password with Zod validation
3. **Submit handler** (`onSubmit`):
   - Sets `isLoading = true`
   - Calls `signInWithEmailAndPassword(auth, email, password)`
   - On success: checks if Firestore user doc exists, creates if not
   - On failure: shows "Invalid email or password" toast
4. **UI**: ShadCN Card with Logo, form fields, "Forgot password?" link (non-functional), submit button with spinner, "Sign up" link

### 8.3 Signup Page

**File**: `src/app/signup/page.tsx`  
**Route**: `/signup`

**Full Logic:**

1. **Auth guard**: Same as login
2. **Form**: Email + Password with same Zod validation
3. **Submit handler** (`onSubmit`):
   - Calls `createUserWithEmailAndPassword(auth, email, password)`
   - Creates Firestore user document with default values
   - Shows "Account Created" success toast
   - On failure: shows error message toast
4. **UI**: Similar to login but with "Create an Account" heading and "Login" link

### 8.4 Dashboard

**File**: `src/app/(main)/dashboard/page.tsx`  
**Route**: `/dashboard`

**This is the user's main hub after login.**

#### Data Sources (all real-time via `onSnapshot`):

1. **User document** (`users/{uid}`) → Study streak, skills mastered, time studied
2. **Roadmaps collection** (`users/{uid}/roadmaps`) → All roadmaps for upcoming lessons calculation

#### Computed Data:

**Upcoming Lessons** — For each roadmap:
```typescript
const allConcepts = roadmap.roadmap.flatMap(module => module.concepts);
const completedConcepts = roadmap.completedConcepts || [];
const progress = (completedConcepts.length / allConcepts.length) * 100;
const nextLesson = allConcepts.find(c => !completedConcepts.includes(c));
```

**Chart Data** — Simulated/pseudo-random progress data for the last 7 months:
```typescript
for (let i = 6; i >= 0; i--) {
  const progress = Math.floor(Math.random() * (85 - 40 + 1)) + 40;
  data.push({ month, progress });
}
```
> **Note**: The chart currently uses random data, not actual historical progress. This is a placeholder for future implementation.

#### UI Sections:

1. **Header**: Gradient title "Dashboard" + "Add Roadmap" button
2. **Stats Cards** (3 cards, staggered animation):
   - Study Streak (🔥 days) — red/orange gradient
   - Skills Mastered (✅ count/25) — green gradient
   - Time Studied (⏰ hours/minutes) — blue gradient
3. **Progress Chart** (Recharts BarChart):
   - 7 months of data with gradient-filled bars
   - Uses `linearGradient` for visual appeal
4. **Upcoming Lessons** (or empty state with "Create Roadmap" CTA):
   - Each lesson card shows: title, progress percentage badge, progress bar, next concept name
   - Clickable → navigates to `/roadmap/{id}`

#### Time Formatting:
```typescript
const formatTime = (minutes: number) => {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${m}m`;
};
```

### 8.5 Roadmap List Page

**File**: `src/app/(main)/roadmap/page.tsx`  
**Route**: `/roadmap`

#### Data Flow:

1. Listens to `users/{uid}/roadmaps` collection ordered by `createdAt` descending (newest first)
2. Real-time updates via `onSnapshot`

#### Features:

- **Grid layout** (1/2/3 columns responsive)
- **Progress calculation**: Same formula as dashboard
- **Color-coded gradient bars** based on progress:
  - ≥80%: Green
  - ≥50%: Blue
  - ≥25%: Amber
  - <25%: Gray
- **Module badges**: Shows first 3 module titles as badges, with "+N more" badge
- **Dropdown menu** per card: Delete action with confirmation dialog
- **Empty state**: Centered card with GitMerge icon and "Create Your First Roadmap" CTA
- **Delete functionality**: `deleteDoc` with toast feedback

### 8.6 Roadmap Creation Wizard

**File**: `src/app/(main)/roadmap/create/page.tsx`  
**Route**: `/roadmap/create`

**This is a multi-step wizard with two steps:**

#### Step 1: Initial Form (`step === 'initial'`)

**Form fields** (all validated with Zod):
- **Learning Goals** (required, min 10 chars) — text input
- **Current Expertise** (required, min 10 chars) — textarea
- **Available Study Time** (required, min 2 chars) — text input
- **Specific Topics** (optional) — textarea

**On submit** (`onInitialSubmit`):
1. Saves form values to state
2. Generates 3 skill assessment quiz questions using `generateQuizAndExplanation` with different contexts:
   - "Question 1 of 3 — Ask a foundational question"
   - "Question 2 of 3 — Ask a slightly more advanced question"
   - "Question 3 of 3 — Ask a practical or application-based question"
3. All 3 questions are generated in parallel via `Promise.all`
4. Transitions to quiz step

#### Step 2: Quiz Assessment (`step === 'quiz'`)

**UI**: 
- 3 multiple-choice questions rendered as RadioGroups
- Each question in a styled container with HelpCircle icon
- "Back" button to return to form
- "Generate My Roadmap" button (disabled until all 3 questions answered)

**On quiz submit** (`onQuizSubmit`):
1. Formats quiz results into a summary string:
   ```
   Question: "What is...?"
   User Answer: "Option A"
   Correct Answer: "Option B"
   ```
2. Prepends user's expertise with `## Skill Assessment Results` section
3. Calls `generatePersonalizedRoadmap` with enhanced expertise
4. Saves the complete result to Firestore:
   ```typescript
   await setDoc(roadmapRef, {
     ...formValues,          // goals, expertise, time, topics
     quizSummary,            // Formatted Q&A
     ...result,              // AI roadmap data
     createdAt: serverTimestamp(),
     completedConcepts: [],  // Empty initially
   });
   ```
5. Navigates to the new roadmap: `/roadmap/{id}`

### 8.7 Roadmap Detail Viewer

**File**: `src/app/(main)/roadmap/[id]/page.tsx`  
**Route**: `/roadmap/{id}`  
**Size**: 896 lines, 44KB — **the most complex page in the application**

#### State Management

This page manages extensive state across multiple features:

```typescript
// Page data
const [roadmap, setRoadmap] = useState<Roadmap | null>(null);
const [isLoadingPage, setIsLoadingPage] = useState(true);
const [error, setError] = useState<string | null>(null);

// Concept explanation
const [activeConcept, setActiveConcept] = useState<string | null>(null);
const [explanation, setExplanation] = useState<GetConceptExplanationOutput | null>(null);
const [isLoadingExplanation, setIsLoadingExplanation] = useState(false);

// Inline concept quiz
const [quizConceptKey, setQuizConceptKey] = useState<string | null>(null);
const [quizData, setQuizData] = useState<QuizAndExplanationOutput | null>(null);
const [quizState, setQuizState] = useState<'idle' | 'loading' | 'ready' | 'answered'>('idle');
const [selectedAnswer, setSelectedAnswer] = useState<string | null>(null);
const [isCorrect, setIsCorrect] = useState<boolean | null>(null);
const [quizFeedback, setQuizFeedback] = useState<string | null>(null);

// Resources (auto-fetched, cached)
const [resources, setResources] = useState<FindResourcesOutput | null>(null);
const [isLoadingResources, setIsLoadingResources] = useState(false);

// Review quiz dialog
const [isReviewQuizOpen, setIsReviewQuizOpen] = useState(false);
const [reviewQuizState, setReviewQuizState] = useState<'idle' | 'loading' | 'ready' | 'submitted' | 'error'>('idle');
const [reviewQuizData, setReviewQuizData] = useState<QuizItem[]>([]);
const [reviewQuizAnswers, setReviewQuizAnswers] = useState<Record<number, string>>({});
const [reviewQuizScore, setReviewQuizScore] = useState<number | null>(null);
```

#### Core Data Flow

1. **Load roadmap**: `onSnapshot` listener on `users/{uid}/roadmaps/{id}`
2. **Auto-fetch resources**: If `roadmap.resources` doesn't exist, automatically calls `findResources` and saves results
3. **Real-time progress**: Changes to `completedConcepts` update the UI instantly via Firestore listener

#### Feature 1: Concept Exploration (`handleConceptClick`)

```
User clicks concept → Is it already active?
├── Yes → Collapse/close the concept panel
└── No → Set as active concept
        → Check Firestore cache for explanation
        ├── Cached → Display immediately
        └── Not cached → Call getConceptExplanation AI flow
                       → Display result
                       → Save to Firestore cache
```

#### Feature 2: Inline Concept Quiz (`ConceptQuiz` component + `handleStartQuiz`)

```
User clicks "Test Your Knowledge" button
→ Call generateQuizAndExplanation AI flow
→ Display question with 4 radio button options
→ User selects answer and clicks "Submit Answer"
→ handleQuizSubmit():
    ├── Correct → Show green "Correct!" alert
    └── Incorrect → Show red "Not quite!" alert
                  → Call generateQuizAndExplanation AGAIN with:
                    - userAnswer (what they picked)
                    - correctAnswer (the right answer)
                  → Display simplified explanation in blue info box
→ "Try another question" button → regenerates
```

#### Feature 3: Progress Tracking (`handleToggleComplete`)

```
User clicks "Complete"/"Completed" button on a concept
├── Currently incomplete → arrayUnion(conceptName) to completedConcepts
└── Currently complete   → arrayRemove(conceptName) from completedConcepts
→ Toast notification confirming the action
→ Progress bar and stats update automatically via onSnapshot
```

#### Feature 4: Review Quiz (`ReviewQuizDialog`)

```
User clicks "Start Review Quiz" (in sidebar)
→ Opens Dialog modal
→ Calls generateReviewQuiz with all completed concept names
→ Generates 10+ questions from those topics
→ User answers all questions via RadioGroups
→ "Submit Quiz" button
→ Scores: count correct / total
→ Shows percentage score with gradient text
→ Options: "Review Answers" or "Take Again"
```

#### Feature 5: Resources Tab

Two tabs at the top: "Roadmap" and "Resources"

Resources tab displays 4 categories in a 2-column grid:
- Learning Resources (BookOpen icon)
- Competitions (Trophy icon)
- News & Journals (Newspaper icon)
- Job Opportunities (Briefcase icon)

Each resource card is clickable, opens in a new tab with ExternalLink icon.

#### Feature 6: Delete Roadmap

AlertDialog confirmation → `deleteDoc` → redirect to `/roadmap` list

#### Utility: Markdown to HTML Converter

```typescript
function markdownToHtml(markdown: string) {
  return markdown
    .replace(/^### (.*$)/gim, '<h3>$1</h3>')      // H3
    .replace(/^## (.*$)/gim, '<h2>$1</h2>')        // H2
    .replace(/^# (.*$)/gim, '<h1>$1</h1>')         // H1
    .replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>')  // Bold
    .replace(/\*(.*)\*/gim, '<em>$1</em>')          // Italic
    .replace(/`([^`]+)`/gim, '<code>$1</code>')     // Inline code
    .replace(/^- (.*$)/gim, '<li>$1</li>')          // List items
    .replace(/(<li>.*<\/li>)/gim, '<ul>$1</ul>')    // Wrap in ul
    .replace(/<\/ul>\n<ul>/gim, '')                  // Merge adjacent uls
    .replace(/\n/g, '<br />');                       // Line breaks
}
```

Used to render AI-generated Markdown explanations as HTML via `dangerouslySetInnerHTML`.

### 8.8 Upload & Material Processing Page

**File**: `src/app/(main)/upload/page.tsx`  
**Route**: `/upload`

#### Upload Flow:

1. **Drag-and-drop zone** (react-dropzone):
   - Accepts: PDF, TXT, DOCX, PNG, JPG/JPEG
   - Single file only (`multiple: false`)
   - Visual feedback on drag hover
2. **File display**: Shows selected file name with remove button
3. **Process button**: Triggers AI processing

#### Processing Flow (`handleProcess`):

```
User clicks "Process Material"
→ FileReader.readAsDataURL(file) → base64 data URI
→ processMaterial({ fileDataUri }) → server action
→ Returns: { summary, quiz, flashcards }
→ Display results in 3 sections
```

#### Results Display:

1. **Summary Card**: Full-width card with the AI-generated summary
2. **Flashcards Grid** (3 columns): Interactive flip cards using CSS 3D transforms:
   ```
   Front: Term (with "Term" label)
   Back: Definition (with "Definition" label)
   Click to flip: [perspective:1000px], [transform:rotateY(180deg)]
   Transition: 500ms
   ```
3. **Quiz Section**: Each question with:
   - RadioGroup for options
   - Individual submit button per question
   - Correct/incorrect visual feedback (green/red borders)
   - For incorrect answers: calls `generateQuizAndExplanation` for an AI explanation
   - Loading state per question while fetching explanation

#### Flashcard Component

The `Flashcard` component uses pure CSS 3D transforms for the flip animation:
- Container: `[perspective:1000px]` for 3D effect
- Inner card: `[transform-style:preserve-3d]` with conditional `[transform:rotateY(180deg)]`
- Front/back faces: `[backface-visibility:hidden]`
- 500ms transition duration

### 8.9 Study Zone (Community)

**File**: `src/app/(main)/study/page.tsx`  
**Route**: `/study`

#### Sections:

1. **Community Stats** (3 cards, hardcoded values):
   - 1.2K+ Active Learners
   - 3.8K+ Discussions
   - 95% Success Rate
   > **Note**: These are static/placeholder values, not computed from actual data.

2. **Community Guidelines** (2-column grid):
   - Be respectful, share knowledge, provide feedback
   - Stay on topic, cite sources, celebrate progress

3. **Discussion Channel**: Renders `<DiscussionChannel channelId="general-chat" />` component (see Section 9.2)

### 8.10 Saved Resources Page

**File**: `src/app/(main)/saved-resources/page.tsx`  
**Route**: `/saved-resources`

#### Data Flow:

1. Listens to `users/{uid}/savedResources` collection ordered by `savedAt` descending
2. Real-time updates via `onSnapshot`

#### Resource Type Mapping:

```typescript
const typeInfo = {
  learning: { icon: Compass, label: 'Learning', color: 'bg-blue-100 text-blue-800' },
  competition: { icon: Trophy, label: 'Competition', color: 'bg-yellow-100 text-yellow-800' },
  news: { icon: Newspaper, label: 'News', color: 'bg-green-100 text-green-800' },
};
```

#### Features:
- Each resource shows: icon (by type), title (clickable), description, saved date
- **Delete button**: Removes from Firestore with `deleteDoc`
- **Open button**: Opens URL in new tab
- **Empty state**: Bookmark icon with "No Saved Resources" message
- **Not logged in state**: "Please Log In" message
- **Error state**: Alert with error message

### 8.11 Search Page

**File**: `src/app/(main)/search/page.tsx`  
**Route**: `/search?q={query}`

This is a thin wrapper that renders the `SearchResults` component (see Section 9.3).

```tsx
import SearchResults from '@/components/search-results';
export default function SearchPage() {
  return <SearchResults />;
}
```

The actual search logic lives in the `SearchResults` component for reusability.

---

## 9. Shared Components

### 9.1 Main Header

**File**: `src/components/main-header.tsx`

**Navigation Items:**
```typescript
const menuItems = [
  { href: "/dashboard", label: "Dashboard", icon: BrainCircuit },
  { href: "/roadmap", label: "Roadmap", icon: GitMerge },
  { href: "/study", label: "Study", icon: BrainCircuit },
  { href: "/upload", label: "Upload", icon: Upload },
];
```

**Features:**
- **Sticky header** with `backdrop-blur` for glass effect
- **Desktop nav**: Horizontal links with active state highlighting (`pathname.startsWith(item.href)`)
- **Mobile nav**: Sheet (slide-out drawer) triggered by hamburger menu, with icons alongside labels
- **Search bar**: Form with search icon, submits to `/search?q={query}` via `router.push`
- **User menu dropdown**:
  - "Saved Resources" link
  - "Settings" (placeholder)
  - "Support" (placeholder)
  - "Logout" button with icon

### 9.2 Discussion Channel

**File**: `src/components/discussion-channel.tsx`

**Real-time Chat Implementation:**

1. **Channel initialization**: On mount, ensures the channel document exists in Firestore using `setDoc` with `{ merge: true }` (creates if missing, preserves if exists)
2. **Message listener**: `onSnapshot` on `discussionChannels/{channelId}/messages` ordered by `createdAt` ascending
3. **Auto-scroll**: After each message update, scrolls to bottom by finding the Radix scroll viewport element and setting `scrollTop = scrollHeight`
4. **Send message**: `addDoc` with text, server timestamp, userId, and userEmail
5. **Avatar generation**: First 2 characters of email, uppercased

**Predefined Channels:**
```typescript
const channelDetails = {
  'calculus-help': { name: "Calculus Help", description: "Discuss calculus problems..." },
  'general-chat': { name: "General Chat", description: "Talk about anything study-related." }
};
```

**UI**: Fixed-height card (70vh) with ScrollArea, message bubbles showing avatar + email + relative time + message text, input + send button at bottom.

### 9.3 Search Results

**File**: `src/components/search-results.tsx`

**Logic:**
1. Reads `q` parameter from URL search params
2. Calls `searchTopic({ topic: query })` AI flow
3. Displays results in a 3-column grid layout:
   - **Left (2 cols)**: Explanation card (Markdown → HTML) + External Resources card
   - **Right (1 col)**: "Test Your Knowledge" sticky card with quiz title and "Start Quiz" button linking to `/study?topic={query}`
4. States: loading skeleton, error card, results view, no-query prompt

### 9.4 Landing Page

**File**: `src/components/landing-page.tsx`  
**Size**: 33KB — the second largest component

This is a full marketing landing page rendered when unauthenticated users visit `/`. It contains:
- Hero section with gradient text and CTA buttons
- Feature highlights with icons and descriptions
- Visual demonstrations of the platform
- Call-to-action sections linking to `/signup`

### 9.5 Page Transition Wrapper

**File**: `src/components/page-transition-wrapper.tsx`

Uses Framer Motion for page transitions within the `(main)` route group:

```tsx
<motion.div
  initial={{ opacity: 0, y: 10 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ duration: 0.3 }}
>
  {children}
</motion.div>
```

Every page within the authenticated area gets a fade-in + slide-up animation on load.

### 9.6 Logo Component

**File**: `src/components/logo.tsx`

Simple text-based logo component rendering "StudyGen" with the headline font and a GraduationCap icon.

---

## 10. Design System

### 10.1 Color Palette (CSS Custom Properties)

Defined in `src/app/globals.css` using HSL values:

| Token | Light Mode | Dark Mode | Usage |
|---|---|---|---|
| `--background` | `231 50% 94%` | `230 25% 10%` | Page background |
| `--foreground` | `231 30% 20%` | `0 0% 98%` | Primary text |
| `--primary` | `231 48% 48%` | `231 50% 68%` | Buttons, links, accents |
| `--secondary` | `231 50% 90%` | `230 25% 20%` | Secondary elements |
| `--accent` | `187 100% 42%` | `187 100% 42%` | Teal accent (same both modes) |
| `--destructive` | `0 84.2% 60.2%` | `0 62.8% 30.6%` | Error states, delete buttons |
| `--muted` | `231 50% 90%` | `230 25% 20%` | Muted backgrounds |
| `--border` | `231 50% 88%` | `230 25% 25%` | Borders |
| `--radius` | `0.5rem` | — | Border radius |

### 10.2 Typography

| Class | Font | Usage |
|---|---|---|
| `font-body` | Inter | All body text |
| `font-headline` | Space Grotesk | Headings, titles, card headers |

### 10.3 Component Design Patterns

Throughout the app, components follow consistent patterns:

**Card Pattern:**
```html
<Card className="bg-white dark:bg-gray-800 border-0 shadow-lg rounded-xl overflow-hidden">
  <div className="h-2 bg-gradient-to-r from-{color}-500 to-{color}-500"></div>  <!-- Color bar -->
  <CardHeader>
    <div className="p-2 rounded-lg bg-{color}-100 dark:bg-{color}-900/30">  <!-- Icon container -->
      <Icon />
    </div>
    <CardTitle />
  </CardHeader>
  <CardContent />
</Card>
```

**Button Gradient Pattern:**
```html
<Button className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 shadow-md">
```

**Animation Pattern (Framer Motion):**
```tsx
<motion.div
  initial={{ opacity: 0, y: 20 }}
  animate={{ opacity: 1, y: 0 }}
  transition={{ delay: index * 0.1 }}  // Staggered
>
```

---

## 11. Data Flow Diagrams

### 11.1 Roadmap Creation Flow

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Step 1:    │    │   Step 2:    │    │   Step 3:    │    │   Step 4:    │
│  User fills  │──▶│  AI generates│──▶│  User answers│───▶│  AI generates│
│  form with   │    │  3 assessment│    │  assessment  │    │  personalized│
│  goals,      │    │  questions   │    │  questions   │    │  roadmap     │
│  expertise,  │    │  (parallel)  │    │              │    │              │
│  time        │    │              │    │              │    │              │
└──────────────┘    └──────────────┘    └──────────────┘    └──────┬───────┘
                                                                   │
                                                                   ▼
                                                            ┌──────────────┐
                                                            │   Step 5:    │
                                                            │  Save to     │
                                                            │  Firestore   │
                                                            │  + redirect  │
                                                            │  to viewer   │
                                                            └──────────────┘
```

### 11.2 Concept Learning Flow

```
┌──────────────┐
│  User clicks │
│  concept in  │
│  accordion   │
└──────┬───────┘
       │
       ▼
┌──────────────┐     ┌──────────────┐
│  Check cache │───▶ │  Cache hit? │
│  in Firestore│     │              │
└──────────────┘     └──────┬───────┘
                     Yes   │   No
                  ┌────────┘   └────────┐
                  ▼                     ▼
           ┌────────────┐       ┌────────────┐
           │  Display   │       │  Call AI   │
           │  cached    │       │ explanation│
           │ explanation│       │  flow      │
           └────────────┘       └──────┬─────┘
                                       │
                                       ▼
                                ┌────────────┐
                                │  Display + │
                                │  cache in  │
                                │  Firestore │
                                └──────┬─────┘
                                       │
                                       ▼
                                ┌────────────┐
                                │  Show quiz │
                                │  button    │
                                └──────┬─────┘
                                       │
                                       ▼
                                ┌────────────┐    ┌──────────┐
                                │  User takes│───▶│ Correct? │
                                │  quiz      │    │          │
                                └────────────┘    └────┬─────┘
                                                 Yes   │  No
                                              ┌────────┘  └────────┐
                                              ▼                    ▼
                                       ┌────────────┐      ┌────────────┐
                                       │  "Correct!"│      │  Call AI   │
                                       │  alert     │      │  for       │
                                       └────────────┘      │ explanation│
                                                           └────────────┘
```

### 11.3 Material Processing Flow

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  User drops  │───▶│  FileReader  │───▶│  Server      │
│  file (PDF,  │    │  .readAs     │    │  Action:     │
│  DOCX, TXT,  │    │  DataURL()   │    │  processMat  │
│  image)      │    │  → base64    │    │  erial()     │
└──────────────┘    └──────────────┘    └──────┬───────┘
                                               │
                                    ┌──────────┼──────────┐
                                    ▼          ▼          ▼
                              ┌──────────┐ ┌────────┐ ┌──────────┐
                              │ Summary  │ │  Quiz  │ │Flashcards│
                              │ Card     │ │ (5-10  │ │ (5-10    │
                              │          │ │ Q&A)   │ │ term/def)│
                              └──────────┘ └───┬────┘ └──────────┘
                                               │
                                               ▼
                                        ┌────────────┐
                                        │  Per-Q     │
                                        │  submit +  │
                                        │  AI        │
                                        │  feedback  │
                                        │  on wrong  │
                                        │  answers   │
                                        └────────────┘
```

---

## 12. Utility Functions

### 12.1 `cn("")` — Class Name Merger

**File**: `src/lib/utils.ts`

```typescript
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

Used throughout the app for conditional class name merging with Tailwind CSS conflict resolution. For example, `cn("text-red-500", condition && "text-blue-500")` — if `condition` is true, `twMerge` ensures only `text-blue-500` is applied (no conflict).

### 12.2 `markdownToHtml()` — Markdown Renderer

Defined in both `roadmap/[id]/page.tsx` and `search-results.tsx` (duplicated):

Converts basic Markdown to styled HTML using regex replacements:
- `#`, `##`, `###` → Styled headings with appropriate font sizes and margins
- `**text**` → `<strong>`
- `*text*` → `<em>`
- `` `code` `` → `<code>` with muted background styling
- `- item` → `<li>` wrapped in `<ul>`
- Newlines → `<br />`

---

## 13. Custom Hooks

### 13.1 `useMobile()`

**File**: `src/hooks/use-mobile.tsx`

```typescript
const MOBILE_BREAKPOINT = 768;

export function useIsMobile() {
  const [isMobile, setIsMobile] = useState<boolean | undefined>(undefined);

  useEffect(() => {
    const mql = window.matchMedia(`(max-width: ${MOBILE_BREAKPOINT - 1}px)`);
    const onChange = () => setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);
    mql.addEventListener("change", onChange);
    setIsMobile(window.innerWidth < MOBILE_BREAKPOINT);
    return () => mql.removeEventListener("change", onChange);
  }, []);

  return !!isMobile;
}
```

Returns `true` when viewport width is below 768px. Uses `matchMedia` for efficient resize detection.

### 13.2 `useToast()`

**File**: `src/hooks/use-toast.ts`

A state management system for toast notifications with the following features:
- **Action types**: `ADD_TOAST`, `UPDATE_TOAST`, `DISMISS_TOAST`, `REMOVE_TOAST`
- **Maximum 1 toast** at a time (`TOAST_LIMIT = 1`)
- **Auto-dismiss**: Toasts automatically dismiss after a configured duration
- **Reducer pattern**: Uses React `useReducer` for predictable state updates
- **Memory management**: Queues dismissed toasts for removal after animation completes
- **Exported API**: `toast()` function and `useToast()` hook that provides `toast()`, `dismiss()`, and current `toasts` array

---

## 14. Deployment & Hosting

### 14.1 Firebase App Hosting

**File**: `apphosting.yaml`

The project is configured for Firebase App Hosting, which can automatically deploy Next.js applications.

### 14.2 Vercel

The project includes `@vercel/speed-insights` and the `SpeedInsights` component in the root layout, indicating it's also set up for Vercel deployment with performance monitoring.

### 14.3 Production Considerations

1. **Environment variables**: Must be configured in the hosting provider's dashboard
2. **Firestore rules**: Must be switched from test mode to the security rules documented in Section 6.4
3. **API quotas**: Gemini API usage should be monitored for cost management
4. **ChunkLoadError handling**: The root layout includes a script that auto-reloads the page if a ChunkLoadError occurs (common in Next.js when new deployments invalidate cached chunks):
   ```javascript
   window.addEventListener('unhandledrejection', event => {
     const error = event.reason;
     if (error?.name === 'ChunkLoadError' || error?.message?.includes('ChunkLoadError')) {
       if (!sessionStorage.getItem('chunk-load-error-reloaded')) {
         sessionStorage.setItem('chunk-load-error-reloaded', 'true');
         window.location.reload();
       }
     }
   });
   ```
   Uses `sessionStorage` to prevent infinite reload loops.

---

## Summary of AI Flow Usage Across Pages

| AI Flow | Roadmap Create | Roadmap Detail | Upload | Search | Study |
|---|:---:|:---:|:---:|:---:|:---:|
| `generatePersonalizedRoadmap` | ✅ | — | — | — | — |
| `getConceptExplanation` | — | ✅ | — | — | — |
| `generateQuizAndExplanation` | ✅ (assessment) | ✅ (concept quiz + feedback) | ✅ (wrong answer feedback) | — | — |
| `generateReviewQuiz` | — | ✅ | — | — | — |
| `processMaterial` | — | — | ✅ | — | — |
| `findResources` | — | ✅ (auto-fetch) | — | — | — |
| `searchTopic` | — | — | — | ✅ | — |
| `adaptLearning` | — | — | — | — | — (not used) |

---
resume bulding dynamic
d
*End of Documentation*
