# GitHub Access Report Service

A Spring Boot service that tells you **which users have access to which repositories** in a GitHub organization.

## How to Run

### Prerequisites
- Java 17+
- Maven
- A GitHub Personal Access Token

### Setup

1. Clone the repo
2. Open `src/main/resources/application.properties`
3. Replace `your_github_token_here` with your actual token:
   ```
   github.token=ghp_yourActualTokenHere
   ```

4. Run the app:
   ```bash
   mvn spring-boot:run
   ```

The server starts on `http://localhost:8080`

---

## How Authentication Works

This service uses a **GitHub Personal Access Token (PAT)** for authentication.

Every request to GitHub API includes:
```
Authorization: Bearer <your_token>
```

### Required Token Permissions
- `repo` - to read private repos
- `read:org` - to list org repositories and members

### How to create a token
1. Go to GitHub → Settings → Developer Settings → Personal Access Tokens → Tokens (classic)
2. Click "Generate new token"
3. Select scopes: `repo`, `read:org`
4. Copy the token and put it in `application.properties`

---

## API Endpoint

### GET `/api/v1/report`

Returns a full access report for a GitHub organization.

**Query Parameters:**
| Parameter | Required | Description |
|-----------|----------|-------------|
| `org`     | Yes      | GitHub organization name |

**Example Request:**
```bash
curl "http://localhost:8080/api/v1/report?org=facebook"
```

**Example Response:**
```json
{
  "organization": "facebook",
  "totalRepos": 3,
  "totalUsers": 2,
  "userAccessMap": {
    "john_doe": [
      {
        "repoName": "backend-service",
        "repoUrl": "https://github.com/facebook/backend-service",
        "role": "admin",
        "private": true
      }
    ],
    "jane_smith": [
      {
        "repoName": "frontend-app",
        "repoUrl": "https://github.com/facebook/frontend-app",
        "role": "write",
        "private": false
      }
    ]
  }
}
```

---

## Design Decisions

**Parallel API calls:** GitHub API has to be called once per repo to get collaborators. For orgs with 100+ repos, calling them one-by-one would be very slow. I used `CompletableFuture` with a fixed thread pool of 10 threads to fetch all repo collaborators in parallel.

**Pagination:** GitHub API returns max 100 items per page. The `GitHubClient` handles pagination automatically — it keeps fetching pages until GitHub returns an empty response.

**Error handling:** If the token doesn't have access to a specific repo's collaborators (403), we log a warning and skip that repo instead of failing the whole report. Other errors (404 org not found, 401 bad token) return proper error responses.

**Thread pool size:** Set to 10 to avoid hitting GitHub's rate limit (5000 requests/hour for authenticated users). This can be adjusted based on org size.

---

## Running Tests

```bash
mvn test
```
