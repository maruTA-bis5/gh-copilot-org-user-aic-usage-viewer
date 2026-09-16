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
    private final BigDecimal additionalBudgetAmount;
    private final boolean preventFurtherUsage;
    private final BigDecimal remainingPool;
    private final BigDecimal usageRatePercent;
    private final BigDecimal additionalBudgetUsedAmount;
    private final BigDecimal additionalBudgetUsageRatePercent;
    private final BigDecimal remainingAdditionalBudgetAmount;
    private final BigDecimal additionalBudgetOverageAmount;
    private final Instant fetchedAt;

    /**
     * Creates an overview and derives its included-pool and additional-budget metrics.
     *
     * @param org organization name
     * @param yearMonth month covered by the overview
     * @param totalGrossQuantity total gross credit quantity
     * @param totalDiscountQuantity total included credit quantity
     * @param totalNetQuantity total additional credit quantity
     * @param totalNetAmount total cost of additional credits
     * @param totalPoolCapacity included credit capacity
     * @param additionalBudgetAmount configured additional-credit budget, or {@code null} if unset
     * @param preventFurtherUsage whether usage stops when the additional budget is exhausted
     * @param fetchedAt time at which the source data was fetched
     * @throws NullPointerException if any reference argument except
     *         {@code additionalBudgetAmount} is {@code null}
     */
    public OrgCreditPoolOverview(String org,
                                 YearMonth yearMonth,
                                 BigDecimal totalGrossQuantity,
                                 BigDecimal totalDiscountQuantity,
                                 BigDecimal totalNetQuantity,
                                 BigDecimal totalNetAmount,
                                 BigDecimal totalPoolCapacity,
                                 BigDecimal additionalBudgetAmount,
                                 boolean preventFurtherUsage,
                                 Instant fetchedAt) {
        this.org = Objects.requireNonNull(org, "org must not be null");
        this.yearMonth = Objects.requireNonNull(yearMonth, "yearMonth must not be null");
        this.totalGrossQuantity = Objects.requireNonNull(totalGrossQuantity, "totalGrossQuantity must not be null");
        this.totalDiscountQuantity = Objects.requireNonNull(totalDiscountQuantity, "totalDiscountQuantity must not be null");
        this.totalNetQuantity = Objects.requireNonNull(totalNetQuantity, "totalNetQuantity must not be null");
        this.totalNetAmount = Objects.requireNonNull(totalNetAmount, "totalNetAmount must not be null");
        this.totalPoolCapacity = Objects.requireNonNull(totalPoolCapacity, "totalPoolCapacity must not be null");
        this.additionalBudgetAmount = additionalBudgetAmount;
        this.preventFurtherUsage = preventFurtherUsage;
        this.fetchedAt = Objects.requireNonNull(fetchedAt, "fetchedAt must not be null");

        this.remainingPool = totalPoolCapacity.subtract(totalDiscountQuantity);
        this.usageRatePercent = totalPoolCapacity.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalDiscountQuantity
                        .multiply(new BigDecimal("100"))
                        .divide(totalPoolCapacity, 4, RoundingMode.HALF_UP);
        this.additionalBudgetUsedAmount = additionalBudgetAmount == null
                ? BigDecimal.ZERO
                : totalNetAmount.min(additionalBudgetAmount);
        this.additionalBudgetUsageRatePercent = additionalBudgetAmount == null
                || additionalBudgetAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : additionalBudgetUsedAmount
                        .multiply(new BigDecimal("100"))
                        .divide(additionalBudgetAmount, 4, RoundingMode.HALF_UP);
        this.remainingAdditionalBudgetAmount = additionalBudgetAmount == null
                ? BigDecimal.ZERO
                : additionalBudgetAmount.subtract(additionalBudgetUsedAmount);
        this.additionalBudgetOverageAmount = additionalBudgetAmount == null
                ? BigDecimal.ZERO
                : totalNetAmount.subtract(additionalBudgetAmount).max(BigDecimal.ZERO);
    }

    /** Creates an overview with no additional-credit budget configured. */
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

    /** Returns the configured additional-credit budget, or {@code null} if none is set. */
    public BigDecimal getAdditionalBudgetAmount() { return additionalBudgetAmount; }

    /** Returns whether additional usage is blocked after the configured budget is exhausted. */
    public boolean isPreventFurtherUsage() { return preventFurtherUsage; }
    public BigDecimal getRemainingPool() { return remainingPool; }
    public BigDecimal getUsageRatePercent() { return usageRatePercent; }

    /**
     * Returns the lesser of the total net amount and configured additional budget,
     * or zero if no budget is set.
     */
    public BigDecimal getAdditionalBudgetUsedAmount() { return additionalBudgetUsedAmount; }

    /**
     * Returns the used additional budget as a percentage rounded to four decimal places,
     * or zero if the budget is unset or zero.
     */
    public BigDecimal getAdditionalBudgetUsageRatePercent() { return additionalBudgetUsageRatePercent; }

    /** Returns the additional budget minus its used amount, or zero if no budget is set. */
    public BigDecimal getRemainingAdditionalBudgetAmount() { return remainingAdditionalBudgetAmount; }

    /** Returns the positive total net amount above the budget, or zero if no budget is set. */
    public BigDecimal getAdditionalBudgetOverageAmount() { return additionalBudgetOverageAmount; }

    /** Returns whether an additional-credit budget is configured. */
    public boolean isAdditionalBudgetSet() { return additionalBudgetAmount != null; }

    /**
     * Returns whether a positive overage should be displayed.
     *
     * <p>Overage is hidden when no additional budget is set or further usage is prevented.</p>
     */
    public boolean isAdditionalBudgetOverageVisible() {
        return isAdditionalBudgetSet()
                && !preventFurtherUsage
                && additionalBudgetOverageAmount.compareTo(BigDecimal.ZERO) > 0;
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
                && Objects.equals(additionalBudgetAmount, that.additionalBudgetAmount)
                && preventFurtherUsage == that.preventFurtherUsage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(org, yearMonth, totalGrossQuantity, totalDiscountQuantity,
                totalNetQuantity, totalNetAmount, totalPoolCapacity,
                additionalBudgetAmount, preventFurtherUsage);
    }

    @Override
    public String toString() {
        return "OrgCreditPoolOverview{org='%s', yearMonth=%s, totalDiscountQuantity=%s, "
                + "totalNetQuantity=%s, totalPoolCapacity=%s, additionalBudgetAmount=%s, "
                + "remainingPool=%s, usageRatePercent=%s%%}"
                .formatted(org, yearMonth, totalDiscountQuantity,
                        totalNetQuantity, totalPoolCapacity, additionalBudgetAmount,
                        remainingPool, usageRatePercent);
    }
}
