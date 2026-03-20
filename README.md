# GitHub Organization Access Report

A Spring Boot REST API that shows **which users have access to which repositories** in a GitHub organization.

---

## How to Run the Project

### Prerequisites
- Java 17+
- Maven 3.6+
- GitHub Personal Access Token

### Setup

**Step 1 - Clone the repository**
```bash
git clone https://github.com/Jbansal2/github-access-report.git
cd github-access-report
```

**Step 2 - Create application.properties**

Create this file at `src/main/resources/application.properties`:
```properties
server.port=8080
github.api.url=https://api.github.com
github.token=YOUR_GITHUB_TOKEN_HERE
```

**Step 3 - Run the project**
```bash
mvn spring-boot:run
```

Server starts at `http://localhost:8080`

---

## How Authentication is Configured

This service uses **GitHub Personal Access Token (PAT)** for authentication.

Every outgoing request to GitHub API automatically includes:
```
Authorization: Bearer <your_token>
Accept: application/vnd.github+json
X-GitHub-Api-Version: 2022-11-28
```

This is handled using a `RestTemplate` interceptor in `GitHubConfig.java` — so every API call gets the auth header automatically without repeating it everywhere.

### How to Create a Token

1. GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)
2. Click **"Generate new token (classic)"**
3. Select these scopes:
   - `repo` - to read repositories
   - `read:org` - to read org members and teams
4. Copy the generated token → paste in `application.properties`

---

## How to Call the API

### Endpoint

```
GET /api/v1/report?org={organization_name}
```

### Example Request
```bash
curl "http://localhost:8080/api/v1/report?org=google"
```

## Assumptions & Design Decisions

### 1. Teams API instead of per-repo Collaborator API

**Problem:** GitHub has a collaborator API (`/repos/{org}/{repo}/collaborators`) but calling it for every repo means:
- 100 repos = 100 API calls
- 3000 repos = 3000 API calls → GitHub rate limit hit

**Solution:** Used the **Teams API** instead:
```
GET /orgs/{org}/teams              → fetch all teams (few calls)
GET /orgs/{org}/teams/{slug}/members → team members
GET /orgs/{org}/teams/{slug}/repos   → team repos
```
This gives the same result in far fewer API calls.

### 2. Parallel API Calls with CompletableFuture

For each team, member and repo calls are fired **in parallel** using `CompletableFuture` with a fixed thread pool of 5 threads. This speeds up report generation significantly for orgs with many teams.

### 3. Automatic Pagination

GitHub API returns max 100 items per page. All API calls in `GitHubClient.java` handle pagination automatically — keeps fetching until GitHub returns an empty page.

### 4. Large Org Behavior

Organizations like Microsoft and Google **restrict team visibility** to org members only. This is expected GitHub security behavior — not a bug. The API returns total repos and members count which are publicly accessible, while `userAccessMap` will be populated when the token belongs to an org member.

### 5. Graceful Error Handling

- `403` on a specific repo/team → logged as warning, rest of report continues
- `404` org not found → proper error response returned
- `401` bad token → clear error message returned

---
