package io.github.marutabis5.copilotviewer.domain.model;

import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Full AI credit usage report for a specific user and calendar month.
 * Daily entries with zero usage are omitted from {@link #getDailyUsages()};
 * use {@link #isEmpty()} to detect an all-zero month.
 */
public final class MonthlyUsageReport {

    private final String org;
    private final String login;
    private final YearMonth yearMonth;
    private final List<DailyUsage> dailyUsages;
    private final Instant fetchedAt;

    public MonthlyUsageReport(String org,
                              String login,
                              YearMonth yearMonth,
                              List<DailyUsage> dailyUsages,
                              Instant fetchedAt) {
        this.org = Objects.requireNonNull(org, "org must not be null");
        this.login = Objects.requireNonNull(login, "login must not be null");
        this.yearMonth = Objects.requireNonNull(yearMonth, "yearMonth must not be null");
        this.dailyUsages = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(dailyUsages, "dailyUsages must not be null")));
        this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
    }

    public String getOrg() { return org; }
    public String getLogin() { return login; }
    public YearMonth getYearMonth() { return yearMonth; }
    public List<DailyUsage> getDailyUsages() { return dailyUsages; }
    public Instant getFetchedAt() { return fetchedAt; }

    /** Sum of net credit quantities across all days in the month. */
    public double getTotalNetQuantity() {
        return dailyUsages.stream().mapToDouble(DailyUsage::getTotalNetQuantity).sum();
    }

    /** Sum of net amounts (cost) across all days in the month. */
    public double getTotalNetAmount() {
        return dailyUsages.stream().mapToDouble(DailyUsage::getTotalNetAmount).sum();
    }

    /** {@code true} when no usage was recorded for any day in the month. */
    public boolean isEmpty() { return dailyUsages.isEmpty(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonthlyUsageReport that)) return false;
        return Objects.equals(org, that.org)
                && Objects.equals(login, that.login)
                && Objects.equals(yearMonth, that.yearMonth)
                && Objects.equals(dailyUsages, that.dailyUsages);
    }

    @Override
    public int hashCode() { return Objects.hash(org, login, yearMonth); }

    @Override
    public String toString() {
        return "MonthlyUsageReport{org='%s', login='%s', yearMonth=%s, days=%d, totalNetQuantity=%.4f}"
                .formatted(org, login, yearMonth, dailyUsages.size(), getTotalNetQuantity());
    }
}
