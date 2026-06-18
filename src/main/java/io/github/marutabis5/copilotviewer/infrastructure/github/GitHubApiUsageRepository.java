package io.github.marutabis5.copilotviewer.infrastructure.github;

import io.github.marutabis5.copilotviewer.domain.model.DailyUsage;
import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.domain.model.UsageItem;
import io.github.marutabis5.copilotviewer.domain.repository.UsageRepository;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.AiCreditUsageResponse;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.UsageItemDto;
import io.github.marutabis5.copilotviewer.service.GitHubApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link UsageRepository} implementation that fetches data in real time from
 * {@code GET /organizations/{org}/settings/billing/ai_credit/usage}.
 *
 * <p>Because the API does not support a date range in a single call, one
 * request is made per calendar day in the requested month.
 * Each request is retried up to <em>2</em> times with exponential back-off
 * (1 s → 2 s) on HTTP 429 or 5xx responses.</p>
 */
@ApplicationScoped
public class GitHubApiUsageRepository implements UsageRepository {

    private static final Logger LOG = Logger.getLogger(GitHubApiUsageRepository.class);

    /** Statuses that warrant a retry. */
    private static final int[] RETRYABLE_STATUSES = {429, 500, 502, 503, 504};

    static final int MAX_RETRIES = 2;
    static final long INITIAL_BACKOFF_MS = 1_000L;

    @Inject
    @RestClient
    GitHubBillingClient billingClient;

    @Override
    public MonthlyUsageReport findByOrgAndUserAndMonth(String org,
                                                       String login,
                                                       YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay  = yearMonth.atEndOfMonth();

        List<DailyUsage> dailyUsages = new ArrayList<>();

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            AiCreditUsageResponse response = fetchDayWithRetry(
                    org, login, date.getYear(), date.getMonthValue(), date.getDayOfMonth());

            List<UsageItem> items = mapItems(response);
            if (!items.isEmpty()) {
                dailyUsages.add(new DailyUsage(date, items));
            }
        }

        return new MonthlyUsageReport(org, login, yearMonth, dailyUsages, Instant.now());
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Calls the GitHub API for a single day, retrying on transient failures
     * with exponential back-off.
     */
    AiCreditUsageResponse fetchDayWithRetry(String org, String login,
                                            int year, int month, int day) {
        long backoffMs = INITIAL_BACKOFF_MS;
        WebApplicationException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return billingClient.getAiCreditUsage(org, year, month, day, login);
            } catch (WebApplicationException ex) {
                int status = ex.getResponse().getStatus();
                lastException = ex;

                if (attempt < MAX_RETRIES && isRetryable(status)) {
                    LOG.warnf("GitHub API HTTP %d for %s/%s %04d-%02d-%02d "
                                    + "(attempt %d/%d). Retrying in %d ms...",
                            status, org, login, year, month, day,
                            attempt + 1, MAX_RETRIES + 1, backoffMs);
                    sleepForTest(backoffMs);
                    backoffMs *= 2;
                } else {
                    String summary = buildSafeSummary(status);
                    LOG.errorf("GitHub API failed for %s/%s %04d-%02d-%02d "
                                    + "after %d retries. HTTP %d: %s",
                            org, login, year, month, day, attempt, status, summary);
                    throw new GitHubApiException(status, summary, ex);
                }
            }
        }

        // Unreachable, but keeps the compiler happy.
        int status = lastException != null ? lastException.getResponse().getStatus() : -1;
        throw new GitHubApiException(status, buildSafeSummary(status));
    }

    private static List<UsageItem> mapItems(AiCreditUsageResponse response) {
        return response.getUsageItems().stream()
                .map(dto -> new UsageItem(
                        nullToEmpty(dto.getProduct()),
                        nullToEmpty(dto.getSku()),
                        nullToEmpty(dto.getModel()),
                        nullToEmpty(dto.getUnitType()),
                        dto.getGrossQuantity(),
                        dto.getDiscountQuantity(),
                        dto.getNetQuantity(),
                        dto.getNetAmount()))
                .toList();
    }

    private static boolean isRetryable(int status) {
        for (int s : RETRYABLE_STATUSES) {
            if (s == status) return true;
        }
        return false;
    }

    /**
     * Returns a brief, safe error summary. Never exposes PAT tokens or
     * full response bodies.
     */
    private static String buildSafeSummary(int status) {
        return switch (status) {
            case 400 -> "Bad request (invalid parameters)";
            case 401 -> "Unauthorized (check PAT token configuration)";
            case 403 -> "Forbidden (PAT lacks Administration:read permission)";
            case 404 -> "Organization not found or API endpoint unavailable";
            case 429 -> "Rate limit exceeded";
            case 500 -> "GitHub internal server error";
            case 503 -> "GitHub service temporarily unavailable";
            default  -> "Unexpected error (HTTP " + status + ")";
        };
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    /**
     * Sleeps for the given number of milliseconds between retries.
     * Overridable in tests (via anonymous subclass) to eliminate real delays.
     */
    void sleepForTest(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
