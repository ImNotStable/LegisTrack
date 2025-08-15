# LegisTrack

LegisTrack is an AI-driven legislative document tracking and exploration platform. It combines a Kotlin/Spring backend, a React + TypeScript frontend, and Docker Compose orchestration to ingest, analyze, and surface legislative documents using modern AI techniques such as natural language processing, semantic embeddings, summarization, and semantic search.

This README gives a concise overview, quickstart instructions, development hints, and notes for contributors.

## Key features (AI-driven)
- Document ingestion pipeline with metadata extraction and normalization
- NLP-powered processing: language detection, tokenization, named-entity extraction, and classification
- Embeddings and semantic search for meaning-based retrieval across documents
- Summarization and question-answering over documents (extractive/abstractive)
- AI-assisted UI: relevance scoring, highlight extraction, and suggested follow-ups

> Note: Specific AI models and providers are pluggable. See the `backend` configuration and environment variables for model/provider selection.

## Architecture

- Backend: Kotlin + Spring (Gradle) — document ingestion, AI orchestration, REST API
- Frontend: React + TypeScript — user interface, document feed, detail and search views
- Orchestration: `docker-compose.yml` for local development and quick deployments

Repository layout (top-level):

- `backend/` — Kotlin/Spring code, Gradle build
- `frontend/` — React + TypeScript app (Create React App / Vite-style layout)
- `docker-compose.yml` — compose setup for backend, frontend (or built assets) and supporting services

## Prerequisites

- Docker & Docker Compose (recommended for first-run)
- Java 17+ (if running backend locally)
- Node.js 18+ and npm/yarn (if running frontend locally)
- (Optional) Access to AI model/service provider (API key or local LLM)

## Quick start (Docker Compose)

From the project root run:

```bash
docker-compose up -d
```

This brings up the backend and frontend (and any configured services). Allow a few moments for services to initialize. Check logs with `docker-compose logs -f`.

Default ports (verify in your local config):
- Backend API: http://localhost:8080
- Frontend (dev): http://localhost:3000 or static assets served by Nginx if using the production build

## Run locally (development)

Backend (Kotlin/Spring):

```bash
cd backend
./gradlew bootRun
```

Look for Spring Boot startup logs and an HTTP listening port (commonly 8080). Configurations are under `backend/src/main/resources` (profiles and `application-*.properties` or `.yml`).

Frontend (React/TypeScript):

```bash
cd frontend
npm install
npm run start
```

The frontend will open a hot-reloading development server (commonly http://localhost:3000) and point API requests to the configured backend URL.

## Configuration & AI provider

The project is designed to keep AI provider selection configurable via environment variables and backend config files. Typical knobs include:

- MODEL_PROVIDER (e.g., OpenAI, local LLM)
- API keys or credentials (set as env vars or docker secrets)
- EMBEDDING_DIM, VECTOR_STORE settings (if using local vector DB)

Check `backend/src/main/resources` and `docker-compose.yml` for exact variable names used in this repository.

## Testing

- Backend: run Gradle tests

```bash
cd backend
./gradlew test
```

- Frontend: run unit tests

```bash
cd frontend
npm test
```

## Contributing

Contributions are welcome. A few guidelines:

- Open an issue to discuss major changes or new AI integrations (models/providers)
- Keep AI model keys and secrets out of source control; use env vars or secrets management
- Write tests for new features (backend unit tests, frontend component tests)
- Document new configuration options and update this README when behavior changes

## Security and privacy

The platform may process sensitive legislative or personal data depending on ingestion sources. Follow these precautions:

- Secure API keys and model credentials
- Configure data retention and deletion policies in the backend
- Audit which external AI services data is sent to and consider on-prem or private models when privacy-sensitive

## Roadmap & ideas

- Plug-and-play AI backends (OpenAI, Anthropic, local LLMs)
- Vector DB integration (e.g., Milvus, Pinecone, Weaviate) for scale and fast semantic retrieval
- Interactive Q&A over legislative corpora with conversational memory
- Document lineage tracking, cross-references and impact analysis

## Where to look in the code

- Backend source: `backend/src/main/kotlin` and resources in `backend/src/main/resources`
- Frontend source: `frontend/src` (components, hooks, services)
- Docker compose orchestration: `docker-compose.yml`

## License & contact

Specify your project's license here (e.g., MIT) and provide contact or maintainer information.

## License

This project is licensed under the MIT License — see the `LICENSE` file in the project root for details.

## Authorship

This project and the initial codebase and documentation were generated with the assistance of an AI coding assistant. The AI authored the initial project scaffold and README content on 2025-08-15. Human review and verification are recommended for production use — check configuration, secrets, and licenses before deploying.

