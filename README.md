# AI Code Review Assistant

An AI-powered full-stack web application that helps developers and students review Java code automatically. Combines traditional static analysis tools with an AI model to catch bugs, security issues, performance concerns, and style problems — and teaches rather than just fixes, using a Socratic-questioning approach for logical bugs.

**Live demo:** https://ai-code-review-assistant-lemon-one.vercel.app

## Features

- **Authentication** — Register, login, update profile, change password (JWT-based)
- **Code Submission** — Upload a `.java` file or paste code directly into the browser
- **Real Compiler Validation** — Code is checked with `javac` before analysis, so invalid/garbage input is rejected with the actual compiler error, not a confusing AI response
- **Static Analysis** — Checkstyle, PMD, and SpotBugs run automatically, with results translated into beginner-friendly explanations
- **AI Code Review** — Powered by Groq (Llama 3.3 70B), flags bugs, security issues, performance problems, and style improvements
  - For logical bugs specifically, the AI asks a guiding question instead of revealing the fix directly (e.g. *"What happens if the array is empty?"*) — designed to help the developer discover the issue themselves
- **Complexity Analysis** — Classes, methods, lines of code, cyclomatic complexity, average method length, maintainability index, plus an AI-estimated Big-O time complexity with reasoning
- **Documentation Generator** — AI-generated class summary and per-method documentation on demand
- **Review Dashboard** — Search, filter by score, view detailed findings, delete individual reviews or whole projects
- **PDF Export** — Download a full review report

## Tech Stack

**Backend**
- Java 21, Spring Boot 3.5
- Spring Security + JWT authentication
- Spring Data JPA / Hibernate
- PostgreSQL (hosted on Supabase, pooled connection)
- Checkstyle, PMD, SpotBugs for static analysis
- Groq API (Llama 3.3 70B Versatile) for AI review and documentation generation
- iText 7 for PDF report generation

**Frontend**
- React (Vite)
- React Router
- Axios

**Deployment**
- Backend: Docker container on Render
- Frontend: Vercel

## Project Structure

```
ai-code-review-assistant/
├── src/main/java/com/codereview/app/
│   ├── controller/      REST endpoints
│   ├── service/         Business logic (analysis, AI, PDF, auth)
│   ├── entity/          JPA entities
│   ├── repository/      Spring Data repositories
│   ├── dto/              Request/response objects
│   ├── security/        JWT filter, JWT utility
│   ├── config/           Spring Security configuration
│   └── exception/       Global exception handling
├── src/main/resources/
│   ├── application.yml
│   └── checkstyle-rules.xml
├── frontend/
│   └── src/
│       ├── pages/        Login, Register, Dashboard, Review Results, History, Profile
│       └── services/     API client (axios)
├── Dockerfile
└── pom.xml
```

## Running Locally

### Backend

Prerequisites: Java 21, Maven, a PostgreSQL database (or a free Supabase project).

1. Clone the repository
2. Set the following environment variables:
   - `CRA_DB_PASSWORD` — your database password
   - `JWT_SECRET` — any long random string used to sign auth tokens
   - `GROQ_API_KEY` — your Groq API key ([console.groq.com](https://console.groq.com))
3. Update `src/main/resources/application.yml` with your database connection details if not using the same Supabase project
4. Run:
   ```
   mvn clean install
   mvn spring-boot:run
   ```
5. Backend runs on `http://localhost:8080`

### Frontend

Prerequisites: Node.js 18+

1. ```
   cd frontend
   npm install
   ```
2. Create a `.env.local` file in `frontend/` with:
   ```
   VITE_API_URL=http://localhost:8080/api
   ```
3. Run:
   ```
   npm run dev
   ```
4. Frontend runs on `http://localhost:5173`

### Static Analysis Tools

- **Checkstyle** and **PMD** are pulled in automatically via Maven — no extra setup needed.
- **SpotBugs** requires the CLI distribution to be downloaded separately and pointed to via the `SPOTBUGS_HOME` environment variable (defaults to a local path for development; the Docker image bundles it automatically for deployment).

## Deployment

- **Backend** is containerized via the included `Dockerfile` and deployed on [Render](https://render.com) as a Docker web service. The image bundles a full JDK (required for the `javac` compile-check and SpotBugs, which needs bytecode) plus the SpotBugs CLI, downloaded during the Docker build.
- **Frontend** is deployed on [Vercel](https://vercel.com), pointed at the Render backend via the `VITE_API_URL` environment variable.
- CORS is configured in `SecurityConfig.java` to explicitly allow the deployed frontend's origin.

## Known Limitations

- Only Java files are currently supported; other languages are detected and rejected with a clear message rather than silently failing.
- Password reset is implemented as an in-app "Change Password" (requires being logged in and knowing the current password) rather than a full email-based forgot-password flow, which would require integrating a separate email service.
- Complexity metrics (classes, methods, cyclomatic complexity) are computed with a lightweight regex-based analyzer rather than a full AST parser, so results are a close estimate rather than a guaranteed-exact figure for unusual code structures.
- The AI-estimated time complexity (Big-O) is a heuristic from the language model's reading of the code, not a formally verified proof.
- Render's free tier spins down after inactivity, so the first request after a period of idle time may take up to a minute to respond while the service wakes up.

## License

This project was built as an internship/training project. No license specified.