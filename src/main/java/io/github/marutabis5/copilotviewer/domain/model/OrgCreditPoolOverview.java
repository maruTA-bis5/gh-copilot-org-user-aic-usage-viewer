package io.github.marutabis5.copilotviewer.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Objects;

/**
 * Organisation-wide AI credit pool overview for a given calendar month.
 *
 * <p>{@link #getRemainingPool()} is derived as
 * {@code totalPoolCapacity - totalDiscountQuantity}.</p>
 *
 * <p>{@link #getUsageRatePercent()} is derived as
 * {@code (totalDiscountQuantity / totalPoolCapacity) * 100}.</p>
 */
public final class OrgCreditPoolOverview implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String org;
    private final YearMonth yearMonth;
    private final BigDecimal totalGrossQuantity;
    private final BigDecimal totalDiscountQuantity;
    private final BigDecimal totalNetQuantity;
    private final BigDecimal totalNetAmount;
    private final BigDecimal totalPoolCapacity;
    private final BigDecimal additionalCreditBudget;
    private final boolean preventFurtherUsage;
    private final BigDecimal remainingPool;
    private final BigDecimal usageRatePercent;
    private final BigDecimal additionalCreditsUsedWithinBudget;
    private final BigDecimal additionalBudgetUsageRatePercent;
    private final BigDecimal remainingAdditionalCredits;
    private final BigDecimal creditBudgetOverage;
    private final Instant fetchedAt;

    public OrgCreditPoolOverview(String org,
                                 YearMonth yearMonth,
                                 BigDecimal totalGrossQuantity,
                                 BigDecimal totalDiscountQuantity,
                                 BigDecimal totalNetQuantity,
                                 BigDecimal totalNetAmount,
                                 BigDecimal totalPoolCapacity,
                                 BigDecimal additionalCreditBudget,
                                 boolean preventFurtherUsage,
                                 Instant fetchedAt) {
        this.org = Objects.requireNonNull(org, "org must not be null");
        this.yearMonth = Objects.requireNonNull(yearMonth, "yearMonth must not be null");
        this.totalGrossQuantity = Objects.requireNonNull(totalGrossQuantity, "totalGrossQuantity must not be null");
        this.totalDiscountQuantity = Objects.requireNonNull(totalDiscountQuantity, "totalDiscountQuantity must not be null");
        this.totalNetQuantity = Objects.requireNonNull(totalNetQuantity, "totalNetQuantity must not be null");
        this.totalNetAmount = Objects.requireNonNull(totalNetAmount, "totalNetAmount must not be null");
        this.totalPoolCapacity = Objects.requireNonNull(totalPoolCapacity, "totalPoolCapacity must not be null");
        this.additionalCreditBudget = additionalCreditBudget;
        this.preventFurtherUsage = preventFurtherUsage;
        this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        this.remainingPool = totalPoolCapacity.subtract(totalDiscountQuantity);
        this.usageRatePercent = totalPoolCapacity.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalDiscountQuantity
                        .multiply(new BigDecimal("100"))
                        .divide(totalPoolCapacity, 4, RoundingMode.HALF_UP);
        this.additionalCreditsUsedWithinBudget = additionalCreditBudget == null
                ? BigDecimal.ZERO
                : totalNetQuantity.min(additionalCreditBudget);
        this.additionalBudgetUsageRatePercent = additionalCreditBudget == null
                || additionalCreditBudget.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : additionalCreditsUsedWithinBudget
                        .multiply(new BigDecimal("100"))
                        .divide(additionalCreditBudget, 4, RoundingMode.HALF_UP);
        this.remainingAdditionalCredits = additionalCreditBudget == null
                ? BigDecimal.ZERO
                : additionalCreditBudget.subtract(additionalCreditsUsedWithinBudget);
        this.creditBudgetOverage = additionalCreditBudget == null
                ? BigDecimal.ZERO
                : totalNetQuantity.subtract(additionalCreditBudget).max(BigDecimal.ZERO);
    }

    public OrgCreditPoolOverview(String org,
                                 YearMonth yearMonth,
                                 BigDecimal totalGrossQuantity,
                                 BigDecimal totalDiscountQuantity,
                                 BigDecimal totalNetQuantity,
                                 BigDecimal totalNetAmount,
                                 BigDecimal totalPoolCapacity,
                                 Instant fetchedAt) {
        this(org, yearMonth, totalGrossQuantity, totalDiscountQuantity, totalNetQuantity,
                totalNetAmount, totalPoolCapacity, null, false, fetchedAt);
    }

    public String getOrg() { return org; }
    public YearMonth getYearMonth() { return yearMonth; }
    public BigDecimal getTotalGrossQuantity() { return totalGrossQuantity; }
    public BigDecimal getTotalDiscountQuantity() { return totalDiscountQuantity; }
    public BigDecimal getTotalNetQuantity() { return totalNetQuantity; }
    public BigDecimal getTotalNetAmount() { return totalNetAmount; }
    public BigDecimal getTotalPoolCapacity() { return totalPoolCapacity; }
    public BigDecimal getAdditionalCreditBudget() { return additionalCreditBudget; }
    public boolean isPreventFurtherUsage() { return preventFurtherUsage; }
    public BigDecimal getRemainingPool() { return remainingPool; }
    public BigDecimal getUsageRatePercent() { return usageRatePercent; }
    public BigDecimal getAdditionalCreditsUsedWithinBudget() { return additionalCreditsUsedWithinBudget; }
    public BigDecimal getAdditionalBudgetUsageRatePercent() { return additionalBudgetUsageRatePercent; }
    public BigDecimal getRemainingAdditionalCredits() { return remainingAdditionalCredits; }
    public BigDecimal getCreditBudgetOverage() { return creditBudgetOverage; }
    public boolean isAdditionalCreditBudgetSet() { return additionalCreditBudget != null; }
    public boolean isCreditBudgetOverageVisible() {
        return isAdditionalCreditBudgetSet() && !preventFurtherUsage;
    }
    public Instant getFetchedAt() { return fetchedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrgCreditPoolOverview that)) return false;
        return Objects.equals(org, that.org)
                && Objects.equals(yearMonth, that.yearMonth)
                && Objects.equals(totalGrossQuantity, that.totalGrossQuantity)
                && Objects.equals(totalDiscountQuantity, that.totalDiscountQuantity)
                && Objects.equals(totalNetQuantity, that.totalNetQuantity)
                && Objects.equals(totalNetAmount, that.totalNetAmount)
                && Objects.equals(totalPoolCapacity, that.totalPoolCapacity)
                && Objects.equals(additionalCreditBudget, that.additionalCreditBudget)
                && preventFurtherUsage == that.preventFurtherUsage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(org, yearMonth, totalGrossQuantity, totalDiscountQuantity,
                totalNetQuantity, totalNetAmount, totalPoolCapacity,
                additionalCreditBudget, preventFurtherUsage);
    }

    @Override
    public String toString() {
        return "OrgCreditPoolOverview{org='%s', yearMonth=%s, totalDiscountQuantity=%s, "
                + "totalNetQuantity=%s, totalPoolCapacity=%s, additionalCreditBudget=%s, "
                + "remainingPool=%s, usageRatePercent=%s%%}"
                .formatted(org, yearMonth, totalDiscountQuantity,
                        totalNetQuantity, totalPoolCapacity, additionalCreditBudget,
                        remainingPool, usageRatePercent);
    }
}
