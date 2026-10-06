CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    why_fits_you TEXT,
    difficulty VARCHAR(50) DEFAULT 'INTERMEDIATE',
    source_label VARCHAR(50) DEFAULT 'Curated',
    confidence VARCHAR(50) DEFAULT 'Verified',
    url TEXT,
    search_query VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_saved_resources (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    resource_id BIGINT NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_resource UNIQUE(user_id, resource_id)
);

CREATE INDEX idx_resources_category ON resources(category);
CREATE INDEX idx_user_saved_resources_user ON user_saved_resources(user_id);

-- Seed curated resources across all 6 categories
INSERT INTO resources (title, category, description, why_fits_you, difficulty, source_label, confidence, url, search_query) VALUES
-- 1. Hackathons
('Devpost Global AI & Web Hackathons', 'Hackathons', 'Browse worldwide virtual and in-person hackathons focused on AI agents, cloud architectures, and web apps with major sponsor prizes.', 'Great for building portfolio-grade projects and collaborating with engineering peers.', 'INTERMEDIATE', 'Curated', 'Verified Platform', 'https://devpost.com/hackathons', 'Devpost hackathons 2026'),
('Major League Hacking (MLH) Global Season', 'Hackathons', 'The official student and developer hackathon league running weekly sprint hackathons with mentor support and workshops.', 'Perfect for hands-on practice, networking with tech recruiters, and testing real-world prototypes.', 'BEGINNER', 'Curated', 'Top Rated', 'https://mlh.io/seasons/2026/events', 'Major League Hacking events'),
('Google Cloud Gen AI Hackathon', 'Hackathons', 'Build generative AI applications using Gemini models, Vertex AI, and cloud-native serverless runtimes.', 'Directly aligns with your AI and full-stack modules for building production RAG and agent systems.', 'ADVANCED', 'AI suggested', '97% Match', 'https://googlecloud.devpost.com', 'Google Cloud generative AI hackathon'),
('ETHGlobal Web3 & Systems Hackathon', 'Hackathons', 'Global hackathon series focused on distributed systems, cryptographic proofs, and decentralized state machines.', 'Challenging systems and distributed consensus project experience.', 'ADVANCED', 'AI suggested', '91% Match', 'https://ethglobal.com', 'ETHGlobal hackathon'),

-- 2. Open Source
('Spring AI & LangChain4j Projects', 'Open source', 'Contribute to Java''s leading generative AI integration frameworks. Has active Good First Issues for starters.', 'Solidifies your Java and Spring Boot ecosystem mastery with real open-source contributions.', 'INTERMEDIATE', 'Curated', 'High Fit', 'https://github.com/spring-projects/spring-ai', 'Spring AI good first issues github'),
('Apache Superset Open Source Data Platform', 'Open source', 'Modern, enterprise-ready business intelligence and data visualization web application built with Python and React.', 'Bridges your full-stack backend skills with complex enterprise data pipelines.', 'ADVANCED', 'AI suggested', '94% Match', 'https://github.com/apache/superset', 'Apache superset contributors guide'),
('VS Code Community Extensions & Tooling', 'Open source', 'Develop language servers, debugger adapters, and productivity extensions for millions of developers.', 'Practical TypeScript, NodeJS, and IDE architecture engineering experience.', 'INTERMEDIATE', 'Curated', 'Verified', 'https://github.com/microsoft/vscode', 'VS Code extension development docs'),
('First Contributions Guide & Practice Repo', 'Open source', 'Hands-on tutorial project that walks you through your first pull request, Git branching, and merge workflows.', 'Essential foundation for submitting clean, well-tested open-source code.', 'BEGINNER', 'Curated', 'Verified', 'https://firstcontributions.github.io', 'First contributions github guide'),

-- 3. Competitive Programming
('LeetCode Weekly & Biweekly Contests', 'Competitive programming', 'Timed 90-minute contests featuring algorithmic challenges ranging from hash maps to dynamic programming and graphs.', 'Sharpens code execution speed and technical interview problem-solving intuition.', 'INTERMEDIATE', 'Curated', 'Top Rated', 'https://leetcode.com/contest', 'LeetCode weekly contest'),
('Codeforces Div 2 & Div 3 Rounds', 'Competitive programming', 'Community-driven competitive programming platform with rating ladders, editorial breakdowns, and virtual rounds.', 'Builds rigorous time and space complexity discipline under tight time limits.', 'ADVANCED', 'Curated', 'High Challenge', 'https://codeforces.com/contests', 'Codeforces contests schedule'),
('AtCoder Beginner Contest (ABC)', 'Competitive programming', 'Consistently well-curated contest series focusing on algorithmic math, greedy algorithms, and data structures.', 'Provides clear, pedagogical problem statements that ramp up smoothly from basic to advanced.', 'BEGINNER', 'Curated', 'Verified', 'https://atcoder.jp', 'AtCoder beginner contest'),
('Kattis Problem Archive', 'Competitive programming', 'Diverse collection of programming competition problems used in collegiate and international contests.', 'Great for practicing clean I/O parsing, edge case validation, and memory optimization.', 'INTERMEDIATE', 'AI suggested', '89% Match', 'https://open.kattis.com', 'Kattis problem archive'),

-- 4. Research Papers
('Attention Is All You Need (Vaswani et al.)', 'Research papers', 'The landmark paper that introduced the Transformer architecture, multi-head self-attention, and revolutionized modern AI.', 'Foundational reading for understanding how modern LLMs and generative agents operate.', 'ADVANCED', 'Curated', 'Foundational', 'https://arxiv.org/abs/1706.03762', 'Attention Is All You Need paper pdf'),
('In Search of an Understandable Consensus Algorithm (Raft)', 'Research papers', 'Ongaro & Ousterhout''s clear, accessible paper introducing the Raft distributed consensus protocol.', 'Crucial conceptual grounding for high-availability distributed systems and database leader election.', 'ADVANCED', 'Curated', 'Essential', 'https://raft.github.io/raft.pdf', 'Raft consensus algorithm paper pdf'),
('Gorilla: Large Language Model Connected with APIs (Patil et al.)', 'Research papers', 'UC Berkeley research showing how LLMs can reliably generate API calls, function arguments, and documentation lookups.', 'Directly relevant to your AI chat tooling and automated agent execution modules.', 'INTERMEDIATE', 'AI suggested', '95% Match', 'https://arxiv.org/abs/2305.15334', 'Gorilla LLM connected with APIs paper'),
('MapReduce: Simplified Data Processing on Large Clusters', 'Research papers', 'Dean & Ghemawat''s classic Google systems paper on distributed batch processing primitives.', 'Master classic parallel computing patterns and fault-tolerant compute cluster architectures.', 'INTERMEDIATE', 'Curated', 'Classic', 'https://research.google/pubs/pub62/', 'MapReduce Google research paper pdf'),

-- 5. Documentation
('Spring Boot Reference Documentation (v3.x / 4.x)', 'Documentation', 'Comprehensive official guide covering auto-configuration, Spring Data JPA, Actuator, and security architecture.', 'The authoritative technical reference for all your backend microservice implementations.', 'INTERMEDIATE', 'Curated', 'Official Standard', 'https://docs.spring.io/spring-boot/docs/current/reference/html/', 'Spring boot reference guide official docs'),
('React 19 Official Documentation & Architecture', 'Documentation', 'Deep guide on React Server Components, Actions, useActionState, Suspense boundaries, and optimal rendering.', 'Essential for building smooth, responsive web applications without redundant re-renders.', 'BEGINNER', 'Curated', 'Official Standard', 'https://react.dev', 'React 19 official documentation react.dev'),
('MDN Web Docs: Web APIs & JavaScript Guide', 'Documentation', 'Mozilla Developer Network documentation on DOM, Fetch API, Web Audio API, Canvas, and CSS Layouts.', 'Invaluable reference for standard browser APIs and modern JavaScript specifications.', 'BEGINNER', 'Curated', 'Definitive', 'https://developer.mozilla.org', 'MDN web docs JavaScript guide'),
('PostgreSQL 16 Performance Tuning & Indexing', 'Documentation', 'Official manual on B-tree indexing, query planner analysis via EXPLAIN ANALYZE, and connection pool sizing.', 'Helps you optimize SQL queries and design resilient database schemas for scale.', 'ADVANCED', 'Curated', 'High Priority', 'https://www.postgresql.org/docs/current/performance-tips.html', 'PostgreSQL performance tips indexing official docs'),

-- 6. Challenges
('Advent of Code Annual Challenge Library', 'Challenges', 'Daily programming puzzles released every December that can be solved in any programming language.', 'Super fun and highly effective for strengthening algorithmic agility and clean code structure.', 'INTERMEDIATE', 'Curated', 'Top Rated', 'https://adventofcode.com', 'Advent of code problem archive'),
('Build Your Own Redis / Git / Docker From Scratch', 'Challenges', 'Step-by-step programming challenges where you build miniature versions of industrial tools from scratch.', 'Provides profound mechanical sympathy for network protocols, file serialization, and OS syscalls.', 'ADVANCED', 'Curated', 'Top Rated', 'https://codecrafters.io', 'Build your own Redis from scratch guide'),
('Cryptopals Crypto Challenges', 'Challenges', 'Practical cryptographic exercises taking you from simple XOR ciphers to breaking CBC padding or RSA math flaws.', 'Instills true defense-in-depth thinking and solid understanding of cryptographic primitives.', 'ADVANCED', 'AI suggested', '92% Match', 'https://cryptopals.com', 'Cryptopals crypto challenges'),
('Frontend Mentor Real-World UI Challenges', 'Challenges', 'Figma design files and specifications to build pixel-perfect, accessible, and responsive web components.', 'Excellent for honing CSS flexbox/grid layouts, responsive breakpoints, and UI polish.', 'BEGINNER', 'Curated', 'Verified', 'https://www.frontendmentor.io', 'Frontend mentor web challenges');
