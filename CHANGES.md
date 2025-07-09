 # LegisTrack Project Changes

This file documents all significant changes to the LegisTrack project. All entries are in reverse chronological order (newest first).

## 2025-07-09 18:47 - [BUGFIX] Corrected AI instructions to reflect actual .env configuration approach
- Affected: .github/copilot-instructions.md
- Impact: Fixed configuration management section to accurately reflect the project's use of .env files with .env.example template, rather than suggesting profile-only approach. Updated environment variable standards to match actual implementation with DATABASE_URL, REDIS_HOST patterns. Ensures AI assistants will properly maintain the existing .env configuration system
- Developer: AI Assistant
- Reason: User requested to ensure current .env file approach is upheld in future operations, discovered instructions incorrectly suggested alternative configuration approach

## 2025-07-09 18:42 - [CONFIG] Updated AI coding instructions based on codebase analysis
- Affected: .github/copilot-instructions.md
- Impact: Enhanced AI guidance with Windows-specific commands, accurate React Query patterns, proper cache key examples, and corrected configuration management approach. Updated build commands for Windows PowerShell environment. Added frontend project structure details and verified backend patterns match actual implementation
- Developer: AI Assistant  
- Reason: Analyzed actual codebase implementation to ensure AI instructions accurately reflect current project patterns, dependencies, and development environment requirements

## 2025-07-09 17:57 - [FEATURE] Enhanced DocumentCard with political support gradient and UI cleanup
- Affected: frontend/src/components/DocumentCard.tsx
- Impact: Added dynamic gradient effect from top-left to bottom-right based on political support (blue → purple → red). Removed sponsors counter display. Cards are now colorless by default when no sponsor data exists. Gradient intensity reflects actual political support percentages
- Developer: AI Assistant
- Reason: User requested visual enhancement to show political support through gradient coloring and simplified UI by removing sponsor counters

## 2025-07-09 17:47 - [REFACTOR] Removed sample test documents from database
- Affected: PostgreSQL database documents table
- Impact: Deleted sample documents (HR1-118 and S1-118) that were added for testing purposes. Database is now clean and ready for production data ingestion
- Developer: AI Assistant
- Reason: User requested removal of test data to return system to clean state

## 2025-07-09 17:44 - [BUGFIX] Fixed empty documents issue and enabled data ingestion
- Affected: backend/src/main/resources/application-docker.properties, .env, database sample data
- Impact: Enabled scheduled data ingestion in docker profile, created .env file with placeholder API keys, added sample documents to database for testing. Fixed issue where no documents were displaying on webpage due to disabled scheduler and missing data
- Developer: AI Assistant
- Reason: Address user report of empty document feed by ensuring data ingestion functionality is properly enabled and providing sample data for immediate testing

## 2025-07-09 16:40 - [BUGFIX] Fixed .gitignore subdirectory targeting
- Affected: .gitignore
- Impact: Added explicit subdirectory patterns for backend/ and frontend/ directories to ensure proper file exclusion. Fixed issues where build artifacts in backend/build/, backend/.gradle/, etc. were not being properly ignored
- Developer: AI Assistant
- Reason: Critical fix to ensure build artifacts in subdirectories are properly excluded from version control, addressing File Management & Cleanup Standards compliance

## 2025-07-09 16:35 - [CONFIG] Enhanced .gitignore with comprehensive file coverage
- Affected: .gitignore
- Impact: Added comprehensive patterns for Kotlin/Gradle build artifacts, enhanced IDE file coverage, added TestContainers, native binaries, and package files. Ensures all build files, temporary files, and generated content are properly ignored
- Developer: AI Assistant
- Reason: Comply with File Management & Cleanup Standards requiring comprehensive .gitignore and removal of all temporary files/build artifacts from version control

## 2025-07-09 16:30 - [REFACTOR] Cleaned up debug statements in build.gradle.kts
- Affected: backend/build.gradle.kts
- Impact: Removed println() debug statements as required by File Management & Cleanup Standards
- Developer: AI Assistant
- Reason: Comply with mandatory requirement to remove debug statements before commits

## 2025-07-09 16:25 - [SECURITY] Fixed hardcoded secrets and added resource limits in docker-compose.yml
- Affected: docker-compose.yml
- Impact: Removed hardcoded API keys, replaced with environment variable references, and added mandatory resource limits for all containers
- Developer: AI Assistant
- Reason: Comply with security requirements forbidding hardcoded secrets and Environment & Configuration Standards requiring resource limits

## 2025-07-09 16:20 - [CONFIG] Pinned exact versions in frontend package.json
- Affected: frontend/package.json
- Impact: Removed floating versions (^) and pinned exact dependency versions as required by Version Management Standards
- Developer: AI Assistant
- Reason: Comply with mandatory requirement to pin exact versions in dependency files for reproducible builds

## 2025-07-09 16:15 - [CONFIG] Created mandatory .env.example template
- Affected: .env.example (new file)
- Impact: Ensures compliance with Environment & Configuration Standards requiring .env.example template
- Developer: AI Assistant
- Reason: Project must follow all coding instructions including mandatory environment configuration template

## 2025-07-09 15:45 - [CONFIG] Created comprehensive AI coding assistant instructions
- Affected: .github/copilot-instructions.md (new file)
- Impact: Established mandatory coding standards, patterns, and enforcement rules for AI assistants
- Developer: AI Assistant
- Reason: Provide clear architectural guidelines and non-negotiable development rules to ensure code quality, security, and maintainability across the project

## 2025-07-09 15:30 - [CONFIG] Initialized project change documentation system
- Affected: CHANGES.md (new file)
- Impact: Centralized change tracking system established as required by coding standards
- Developer: AI Assistant
- Reason: Implement mandatory change documentation requirements to maintain comprehensive project audit trail
