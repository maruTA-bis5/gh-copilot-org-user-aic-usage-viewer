package io.github.marutabis5.copilotviewer.domain.repository;

import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;

import java.time.YearMonth;

/**
 * Port for fetching/storing per-user Copilot AI credit usage data.
 *
 * <p>For the MVP, only {@link io.github.marutabis5.copilotviewer.infrastructure.github.GitHubApiUsageRepository}
 * is used (real-time GitHub API). A future
 * {@link io.github.marutabis5.copilotviewer.infrastructure.sqlite.SqliteUsageRepository}
 * will be wired in for past months (read-through cache).</p>
 */
public interface UsageRepository {

    /**
     * Retrieves the monthly AI credit usage report for the given user in the given org.
     *
     * @param org       GitHub organisation name (case-insensitive)
     * @param login     GitHub user login (case-insensitive)
     * @param yearMonth the target month
     * @return a {@link MonthlyUsageReport}; never {@code null}
     * @throws io.github.marutabis5.copilotviewer.service.GitHubApiException if the upstream
     *         API call fails after all retries
     */
    MonthlyUsageReport findByOrgAndUserAndMonth(String org, String login, YearMonth yearMonth);
}
