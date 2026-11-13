package io.github.marutabis5.copilotviewer.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
public final class MonthlyUsageReport implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String org;
    private final String login;
    private final YearMonth yearMonth;
    private final List<DailyUsage> dailyUsages;
    private final Instant fetchedAt;
    private final BigDecimal includedCredits;
    private final boolean includedCreditsUnavailable;
    private final BigDecimal userBudgetCapacityCredits;
    private final BigDecimal userBudgetConsumedCredits;
    private final boolean universalBudget;
    private final boolean userBudgetLookupUnavailable;
    private final BigDecimal remainingUserBudgetCredits;
    private final BigDecimal userBudgetUsageRatePercent;

    public MonthlyUsageReport(String org,
                              String login,
                              YearMonth yearMonth,
                              List<DailyUsage> dailyUsages,
                              Instant fetchedAt) {
        this(org, login, yearMonth, dailyUsages, fetchedAt, null);
    }

    public MonthlyUsageReport(String org,
                              String login,
                              YearMonth yearMonth,
                              List<DailyUsage> dailyUsages,
                              Instant fetchedAt,
                              BigDecimal includedCredits) {
        this(org, login, yearMonth, dailyUsages, fetchedAt, includedCredits, false);
    }

    public MonthlyUsageReport(String org,
                              String login,
                              YearMonth yearMonth,
                              List<DailyUsage> dailyUsages,
                              Instant fetchedAt,
                              BigDecimal includedCredits,
                              boolean includedCreditsUnavailable) {
        this(org, login, yearMonth, dailyUsages, fetchedAt, includedCredits,
                includedCreditsUnavailable, null);
    }

    public MonthlyUsageReport(String org,
                              String login,
                              YearMonth yearMonth,
                              List<DailyUsage> dailyUsages,
                              Instant fetchedAt,
                              BigDecimal includedCredits,
                              boolean includedCreditsUnavailable,
                              BigDecimal userBudgetCapacityCredits) {
        this(org, login, yearMonth, dailyUsages, fetchedAt, includedCredits,
                includedCreditsUnavailable, userBudgetCapacityCredits, null, false, false);
    }

    public MonthlyUsageReport(String org,
                              String login,
                              YearMonth yearMonth,
                              List<DailyUsage> dailyUsages,
                              Instant fetchedAt,
                              BigDecimal includedCredits,
                              boolean includedCreditsUnavailable,
                              BigDecimal userBudgetCapacityCredits,
                              BigDecimal userBudgetConsumedCredits,
                              boolean universalBudget) {
        this(org, login, yearMonth, dailyUsages, fetchedAt, includedCredits,
                includedCreditsUnavailable, userBudgetCapacityCredits,
                userBudgetConsumedCredits, universalBudget, false);
    }

    public MonthlyUsageReport(String org,
                              String login,
                              YearMonth yearMonth,
                              List<DailyUsage> dailyUsages,
                              Instant fetchedAt,
                              BigDecimal includedCredits,
                              boolean includedCreditsUnavailable,
                              BigDecimal userBudgetCapacityCredits,
                              BigDecimal userBudgetConsumedCredits,
                              boolean universalBudget,
                              boolean userBudgetLookupUnavailable) {
        if (includedCreditsUnavailable && includedCredits != null) {
            throw new IllegalArgumentException("Unavailable included credits must not have a value");
        }
        this.org = Objects.requireNonNull(org, "org must not be null");
        this.login = Objects.requireNonNull(login, "login must not be null");
        this.yearMonth = Objects.requireNonNull(yearMonth, "yearMonth must not be null");
        this.dailyUsages = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(dailyUsages, "dailyUsages must not be null")));
        this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");
        this.includedCredits = includedCredits;
        this.includedCreditsUnavailable = includedCreditsUnavailable;
        this.userBudgetCapacityCredits = userBudgetCapacityCredits;
        this.userBudgetConsumedCredits = userBudgetConsumedCredits;
        this.universalBudget = universalBudget;
        this.userBudgetLookupUnavailable = userBudgetLookupUnavailable;
        if (userBudgetCapacityCredits == null) {
            this.remainingUserBudgetCredits = null;
            this.userBudgetUsageRatePercent = null;
        } else {
            BigDecimal consumedCredits = userBudgetConsumedCredits != null
                    ? userBudgetConsumedCredits
                    : totalGrossQuantity();
            BigDecimal usedWithinBudget = consumedCredits.min(userBudgetCapacityCredits);
            this.remainingUserBudgetCredits = userBudgetCapacityCredits.subtract(usedWithinBudget);
            this.userBudgetUsageRatePercent = userBudgetCapacityCredits.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.valueOf(100)
                    : usedWithinBudget.multiply(new BigDecimal("100"))
                            .divide(userBudgetCapacityCredits, 4, RoundingMode.HALF_UP);
        }
    }

    public String getOrg() { return org; }
    public String getLogin() { return login; }
    public YearMonth getYearMonth() { return yearMonth; }
    public List<DailyUsage> getDailyUsages() { return dailyUsages; }
    public Instant getFetchedAt() { return fetchedAt; }

    /** Allocated credits for the current month, or null when not calculated. */
    public BigDecimal getIncludedCredits() { return includedCredits; }

    /** Whether the current month's allocation could not be determined. */
    public boolean isIncludedCreditsUnavailable() { return includedCreditsUnavailable; }

    /** Configured user-level budget in AI credits, or {@code null} if none is set. */
    public BigDecimal getUserBudgetCapacityCredits() { return userBudgetCapacityCredits; }

    /** Budget consumption from the API in AI credits, or {@code null} when derived from user usage. */
    public BigDecimal getUserBudgetConsumedCredits() { return userBudgetConsumedCredits; }

    /** Whether the applicable budget is a universal multi-user budget. */
    public boolean isUniversalBudget() { return universalBudget; }

    /** Whether the applicable budget could not be retrieved. */
    public boolean isUserBudgetLookupUnavailable() { return userBudgetLookupUnavailable; }

    /** Whether the configured user budget currently blocks further usage. */
    public boolean isUserBudgetBlocked() {
        return userBudgetCapacityCredits != null
                && (userBudgetCapacityCredits.compareTo(BigDecimal.ZERO) == 0
                || (userBudgetConsumedCredits != null
                ? userBudgetConsumedCredits
                : totalGrossQuantity()).compareTo(userBudgetCapacityCredits) >= 0);
    }

    /** Remaining user-level budget credits, or {@code null} if no budget is set. */
    public BigDecimal getRemainingUserBudgetCredits() { return remainingUserBudgetCredits; }

    /** User-level budget usage rate, or {@code null} if no budget is set. */
    public BigDecimal getUserBudgetUsageRatePercent() { return userBudgetUsageRatePercent; }

    /** Whether a user-level budget is configured. */
    public boolean isUserBudgetSet() { return userBudgetCapacityCredits != null; }

    /** Sum of gross credit quantities across all days in the month. */
    public double getTotalGrossQuantity() {
        return dailyUsages.stream().mapToDouble(DailyUsage::getTotalGrossQuantity).sum();
    }

    /** Sum of discount credit quantities across all days in the month. */
    public double getTotalDiscountQuantity() {
        return dailyUsages.stream().mapToDouble(DailyUsage::getTotalDiscountQuantity).sum();
    }

    /** Sum of net amounts (cost) across all days in the month. */
    public double getTotalNetAmount() {
        return dailyUsages.stream().mapToDouble(DailyUsage::getTotalNetAmount).sum();
    }

    private BigDecimal totalGrossQuantity() {
        return dailyUsages.stream()
                .flatMap(day -> day.getItems().stream())
                .map(item -> BigDecimal.valueOf(item.getGrossQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
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
                && Objects.equals(includedCredits, that.includedCredits)
                && includedCreditsUnavailable == that.includedCreditsUnavailable
                && Objects.equals(userBudgetCapacityCredits, that.userBudgetCapacityCredits)
                && Objects.equals(userBudgetConsumedCredits, that.userBudgetConsumedCredits)
                && universalBudget == that.universalBudget
                && userBudgetLookupUnavailable == that.userBudgetLookupUnavailable
                && Objects.equals(dailyUsages, that.dailyUsages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(org, login, yearMonth, userBudgetCapacityCredits,
                userBudgetConsumedCredits, universalBudget, userBudgetLookupUnavailable);
    }

    @Override
    public String toString() {
        return "MonthlyUsageReport{org='%s', login='%s', yearMonth=%s, days=%d, totalGrossQuantity=%.4f}"
                .formatted(org, login, yearMonth, dailyUsages.size(), getTotalGrossQuantity());
    }
}
