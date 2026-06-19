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
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link UsageRepository} implementation that fetches data in real time from
 * {@code GET /organizations/{org}/settings/billing/ai_credit/usage}.
 *
 * <p>Because the API does not support a date range in a single call, one
 * request is made per calendar day in the requested month.
 * Each request is retried up to <em>2</em> times with a 1-second delay
 * on HTTP 429 or 5xx responses using MicroProfile Fault Tolerance {@link Retry}.</p>
 */
@ApplicationScoped
public class GitHubApiUsageRepository implements UsageRepository {

    private static final Logger LOG = Logger.getLogger(GitHubApiUsageRepository.class);

    /** Statuses that warrant a retry (delegated to {@link Retry} via rethrowing). */
    private static boolean isRetryable(int status) {
        return status == 429 || (status >= 500 && status <= 599);
    }

    @Inject
    @RestClient
    GitHubBillingClient billingClient;

    /** Self-reference via CDI proxy so that {@link Retry} interceptors are applied. */
    @Inject
    GitHubApiUsageRepository self;

    @Override
    public MonthlyUsageReport findByOrgAndUserAndMonth(String org,
                                                       String login,
                                                       YearMonth yearMonth) {
        LocalDate firstDay = yearMonth.atDay(1);
        LocalDate lastDay  = yearMonth.atEndOfMonth();

        List<DailyUsage> dailyUsages = new ArrayList<>();

        for (LocalDate date = firstDay; !date.isAfter(lastDay); date = date.plusDays(1)) {
            AiCreditUsageResponse response;
            try {
                response = self.fetchDayWithRetry(
                        org, login, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
            } catch (WebApplicationException ex) {
                // Retries exhausted – convert to a safe exception with no token exposure.
                int status = ex.getResponse().getStatus();
                String summary = buildSafeSummary(status);
                LOG.errorf("GitHub API failed for %s/%s %s after retries. HTTP %d: %s",
                        org, login, date, status, summary);
                throw new GitHubApiException(status, summary, ex);
            }

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
     * Calls the GitHub API for a single day.
     * Retryable failures (HTTP 429 / 5xx) rethrow the {@link WebApplicationException}
     * so that the MicroProfile Fault Tolerance {@link Retry} interceptor can retry.
     * Non-retryable failures (other 4xx) throw {@link GitHubApiException} directly,
     * which is not in {@code retryOn} and therefore aborts immediately.
     */
    @Retry(maxRetries = 2, delay = 1_000, delayUnit = ChronoUnit.MILLIS,
           retryOn = WebApplicationException.class, jitter = 0)
    AiCreditUsageResponse fetchDayWithRetry(String org, String login,
                                            int year, int month, int day) {
        try {
            return billingClient.getAiCreditUsage(org, year, month, day, login);
        } catch (WebApplicationException ex) {
            int status = ex.getResponse().getStatus();
            if (isRetryable(status)) {
                LOG.warnf("GitHub API HTTP %d for %s/%s %04d-%02d-%02d, will retry...",
                        status, org, login, year, month, day);
                throw ex;
            }
            String summary = buildSafeSummary(status);
            LOG.errorf("GitHub API non-retryable error for %s/%s %04d-%02d-%02d. HTTP %d: %s",
                    org, login, year, month, day, status, summary);
            throw new GitHubApiException(status, summary, ex);
        }
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
}
