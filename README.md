# AI Code Review Assistant

A web app that helps developers review code and pull requests using automated checks, linters, and reviewer suggestions.

## Features
- Inspect pull requests and diffs
- Run automated linters and static analysis
- Suggest fixes and create review comments
- Web UI built with React + Vite (frontend/)

## Quick start
Prerequisites: Node 16+, npm or yarn, Git, (optional) a GitHub token for integrations.

Frontend:
1. cd frontend
2. npm install
3. npm run dev           # start development server
4. npm run build         # produce production build

Backend / Integrations:
- If a backend exists, check its README (backend/) for start instructions.
- Typical env vars used by integrations:
  - GITHUB_TOKEN - token for GitHub API access
  - DATABASE_URL - DB connection string
  - PORT - server port

## Repository layout
- frontend/    React + Vite web client
- backend/     (optional) API, analysis workers, integrations
- docs/        documentation
- scripts/     helper scripts

## Configuration
- Use a .env file or environment variables for secrets.
- Enable TypeScript and type-aware linting for better checks (recommended).

## Contributing
1. Open an issue to discuss major changes.
2. Create a feature branch, make changes, and submit a pull request.
3. Run existing tests and linters before submitting.

## License
See the LICENSE file in the repository (if present).

## Questions
Ask in Issues or add a PR with suggested improvements.
