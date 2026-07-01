package io.github.marutabis5.copilotviewer.service;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Calculates the total AI credit pool capacity for an organisation in a given month.
 *
 * <h2>Promotional period (June–August 2026)</h2>
 * <ul>
 *   <li>Business plan: 3,000 credits per seat</li>
 *   <li>Enterprise plan: 7,000 credits per seat</li>
 * </ul>
 *
 * <h2>Standard period (September 2026 onwards)</h2>
 * <ul>
 *   <li>Business plan: 1,900 credits per seat</li>
 *   <li>Enterprise plan: 3,900 credits per seat</li>
 * </ul>
 *
 * <p>Plan type comparison is case-insensitive — the GitHub API returns lowercase
 * values such as {@code "business"} and {@code "enterprise"}.</p>
 */
public final class PoolCapacityCalculator {

    private static final YearMonth PROMO_START   = YearMonth.of(2026, 6);
    private static final YearMonth PROMO_END     = YearMonth.of(2026, 8);

    private static final int PROMO_BUSINESS    = 3_000;
    private static final int PROMO_ENTERPRISE  = 7_000;
    private static final int STD_BUSINESS      = 1_900;
    private static final int STD_ENTERPRISE    = 3_900;

    private PoolCapacityCalculator() {}

    /**
     * Computes total pool capacity ({@code totalSeats * creditsPerSeat}).
     *
     * @param planType   Copilot plan type (case-insensitive; "business" or "enterprise")
     * @param totalSeats number of licensed seats
     * @param yearMonth  the month for which capacity is being computed
     * @return total credit pool capacity; never {@code null}
     * @throws IllegalArgumentException if {@code planType} is not recognised
     */
    public static BigDecimal calculatePoolCapacity(String planType,
                                                   int totalSeats,
                                                   YearMonth yearMonth) {
        boolean isPromo = !yearMonth.isBefore(PROMO_START) && !yearMonth.isAfter(PROMO_END);

        int creditsPerSeat;
        if ("enterprise".equalsIgnoreCase(planType)) {
            creditsPerSeat = isPromo ? PROMO_ENTERPRISE : STD_ENTERPRISE;
        } else if ("business".equalsIgnoreCase(planType)) {
            creditsPerSeat = isPromo ? PROMO_BUSINESS : STD_BUSINESS;
        } else {
            throw new IllegalArgumentException(
                    "Unknown Copilot plan type: '%s'. Expected 'business' or 'enterprise'."
                    .formatted(planType));
        }

        return BigDecimal.valueOf((long) totalSeats * creditsPerSeat);
    }
}
