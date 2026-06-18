package io.github.marutabis5.copilotviewer.service;

import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.domain.repository.UsageRepository;
import io.github.marutabis5.copilotviewer.infrastructure.github.GitHubApiUsageRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Application service that orchestrates input validation, repository selection,
 * and structured logging for Copilot AI credit usage look-ups.
 *
 * <h2>Repository selection (future extension)</h2>
 * <ul>
 *   <li>Current month (UTC) → {@link GitHubApiUsageRepository} (real-time)</li>
 *   <li>Past month → {@link GitHubApiUsageRepository} for MVP;
 *       will be replaced by {@code SqliteUsageRepository} (read-through cache)
 *       in a future sprint</li>
 * </ul>
 */
@ApplicationScoped
public class CopilotUsageService {

    private static final Logger LOG = Logger.getLogger(CopilotUsageService.class);
    private static final int LOGIN_MAX_LENGTH = 39;   // GitHub hard limit
    private static final DateTimeFormatter YEAR_MONTH_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM");

    @Inject
    GitHubApiUsageRepository apiRepository;

    @Inject
    @ConfigProperty(name = "github.org")
    String org;

    /**
     * Validates inputs, selects the appropriate repository, fetches usage,
     * and logs structured audit information.
     *
     * @param loginRaw     raw login string entered by the user (trimmed internally)
     * @param yearMonthRaw raw year-month string in {@code YYYY-MM} format
     * @return non-null {@link MonthlyUsageReport} (may be empty if no usage was recorded)
     * @throws ValidationException  if any input fails validation
     * @throws GitHubApiException   if the upstream API call fails after all retries
     */
    public MonthlyUsageReport findUsage(String loginRaw, String yearMonthRaw) {
        String login    = validateLogin(loginRaw);
        YearMonth ym    = validateYearMonth(yearMonthRaw);

        UsageRepository repository = selectRepository(ym);

        LOG.infof("Usage query started: org=%s, user=%s, yearMonth=%s",
                org, login, ym);
        long startNs = System.nanoTime();

        MonthlyUsageReport report = repository.findByOrgAndUserAndMonth(org, login, ym);

        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;
        LOG.infof("Usage query completed: org=%s, user=%s, yearMonth=%s, "
                        + "activeDays=%d, totalNetQuantity=%.4f, elapsedMs=%d",
                org, login, ym,
                report.getDailyUsages().size(),
                report.getTotalNetQuantity(),
                elapsedMs);

        return report;
    }

    // -------------------------------------------------------------------------
    // Validation
    // -------------------------------------------------------------------------

    static String validateLogin(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("GitHub login must not be empty.");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > LOGIN_MAX_LENGTH) {
            throw new ValidationException(
                    "GitHub login must not exceed %d characters.".formatted(LOGIN_MAX_LENGTH));
        }
        return trimmed;
    }

    static YearMonth validateYearMonth(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Year-month must not be empty.");
        }
        YearMonth ym;
        try {
            ym = YearMonth.parse(raw.trim(), YEAR_MONTH_FMT);
        } catch (DateTimeParseException e) {
            throw new ValidationException(
                    "Year-month must be in YYYY-MM format (e.g. 2025-06).");
        }
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        if (ym.isAfter(currentMonth)) {
            throw new ValidationException(
                    "Future months are not allowed. "
                    + "Please enter a month up to %s (current UTC month).".formatted(currentMonth));
        }
        return ym;
    }

    // -------------------------------------------------------------------------
    // Repository selection
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link UsageRepository} appropriate for the requested month.
     *
     * <p>In the MVP, the GitHub API repository is always used. When the
     * SQLite read-through cache is implemented, past months will be routed to
     * {@code SqliteUsageRepository} instead.</p>
     */
    private UsageRepository selectRepository(YearMonth yearMonth) {
        // MVP: always use the real-time GitHub API.
        // Future: if (!yearMonth.equals(YearMonth.now(ZoneOffset.UTC))) return sqliteRepository;
        return apiRepository;
    }
}
