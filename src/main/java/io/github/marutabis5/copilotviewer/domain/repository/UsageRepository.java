package io.github.marutabis5.copilotviewer.domain.repository;

import io.github.marutabis5.copilotviewer.domain.model.CopilotBillingInfo;
import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.domain.model.OrgCreditPoolOverview;

import java.time.YearMonth;
import java.util.Optional;

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

    /**
     * Retrieves aggregated organisation-wide AI credit usage for the entire
     * given month.
     *
     * @param org       GitHub organisation name
     * @param yearMonth the target month
     * @return an {@link OrgCreditPoolOverview} with aggregated totals; never {@code null}
     * @throws io.github.marutabis5.copilotviewer.service.GitHubApiException if the upstream
     *         API call fails after all retries
     */
    OrgCreditPoolOverview findOrgCreditPoolUsage(String org, YearMonth yearMonth);

    /**
     * Retrieves the Copilot billing subscription information for the given org.
     *
     * @param org GitHub organisation name
     * @return an {@link Optional} containing the billing info, or empty if unavailable
     * @throws io.github.marutabis5.copilotviewer.service.GitHubApiException if the upstream
     *         API call fails after all retries
     */
    Optional<CopilotBillingInfo> findCopilotBillingInfo(String org);
}
