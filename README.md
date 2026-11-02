# gh-copilot-org-user-aic-usage-viewer

Per-user GitHub Copilot AI-credit usage viewer for a single GitHub Organization.

## Prerequisites

| Tool | Version |
|------|---------|
| Java | 21 (tested with Eclipse Temurin 21) |
| Maven | 3.9+ |

> **Note:** The build requires Java 21. Make sure `JAVA_HOME` points to a JDK 21 installation.
> ```sh
> export JAVA_HOME=/path/to/jdk-21
> export PATH=$JAVA_HOME/bin:$PATH
> ```

## Configuration

The application reads the following values via **MicroProfile Config** (environment variables take precedence over the properties file):

| Property | Environment variable | Description |
|----------|---------------------|-------------|
| `github.pat` | `GITHUB_PAT` | Personal Access Token with **Organization administration: read** permission |
| `github.org` | `GITHUB_ORG` | Target GitHub Organization login (e.g. `my-org`) |

Edit `src/main/resources/META-INF/microprofile-config.properties` to supply defaults, or export the environment variables before running.

## Build

```sh
# Compile and run all tests
mvn test

# Package the WildFly Bootable JAR (skips tests)
mvn package -Dmaven.test.skip=true
```

The Bootable JAR is produced at `target/gh-copilot-org-user-aic-usage-viewer-bootable.jar`.

## Run

```sh
export GITHUB_PAT=ghp_your_token_here
export GITHUB_ORG=your-org-name

java -jar target/gh-copilot-org-user-aic-usage-viewer-bootable.jar
```

The application starts on **http://localhost:8080**. Open it in a browser and:

1. Enter a GitHub login name.
2. Enter a month in `YYYY-MM` format (current month or earlier).
3. Click **Search**.

## Architecture

```
domain/
  model/          – UsageItem, DailyUsage, MonthlyUsageReport (plain Java)
  repository/     – UsageRepository interface

infrastructure/
  github/         – GitHubBillingClient (MicroProfile REST Client)
                    GitHubApiUsageRepository (live API, fixed-delay retry)
  sqlite/         – SqliteUsageRepository (stub; ready for future implementation)

service/
  CopilotUsageService  – input validation, repository selection, INFO/ERROR logging
  GitHubApiException   – carries HTTP status + safe summary (no token exposure)
  ValidationException  – invalid login / year-month

faces/
  UsageBean            – JSF @ViewScoped backing bean

webapp/
  index.xhtml          – PrimeFaces search form + result table
```

### Key design decisions

* **`UsageRepository` interface** – swappable between `GitHubApiUsageRepository` (MVP) and a future `SqliteUsageRepository` without touching the service layer.
* **Per-day API calls** – the GitHub AI-credit usage API (`GET /organizations/{org}/settings/billing/ai_credit/usage`) is called once per day of the requested month to produce a daily breakdown.
* **Fixed-delay retry** – up to 2 retries with a fixed 1-second delay on HTTP 429 / 5xx; non-retryable errors (4xx except 429) are surfaced immediately.
* **Token never exposed** – the PAT is injected server-side only; it is masked in logs and never sent to the browser.

## API reference

`GET /organizations/{org}/settings/billing/ai_credit/usage`  
GitHub API version: **2026-03-10**  
Required permission: **Organization administration: read**

`GET /organizations/{org}/settings/billing/budgets`  
GitHub API version: **2026-03-10**  
Required permission: **Organization administration: read**

The budgets API returns `budget_amount` in USD. The organization credit pool
converts it at **1 USD = 100 AI credits** before calculating or displaying
additional-budget usage.

`GET /orgs/{org}/copilot/billing/seats` (paginated) and
`GET /orgs/{org}/members/{username}/copilot` supply seat assignment timestamps.
Included credits are prorated per seat from its UTC assignment date through the
last day of the month, inclusive, rounding down to whole AI credits before
summing the organization pool. For example, a Business seat assigned on
November 2, 2026 receives `floor(1900 * 29 / 30) = 1836` credits; seats assigned
on or before November 1 receive 1900 credits.

The user page displays these seat included credits separately from included
credits already used, including when no usage has been recorded. They count
toward the shared organization pool rather than limiting that user's usage.
Seat included credits are displayed only
for the current UTC month: the seat APIs return current assignments, not a
historical record of removed or reassigned seats. If Copilot billing information
is unavailable (an HTTP 404 or empty response), the user page still displays
usage and shows the seat included credits as "Unavailable", with a warning and a diagnostic
log. Other API errors are reported normally.

The organization pool page calculates included capacity, remaining credits, and
usage rate only for the current UTC month. For past months these values are
unavailable, and current seat assignments are not queried; historical usage and
the additional-budget information remain visible after billing and plan validation.

## License

[MIT](LICENSE) © 2026 Takayuki Maruyama
