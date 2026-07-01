package io.github.marutabis5.copilotviewer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PoolCapacityCalculator#calculatePoolCapacity}.
 */
class PoolCapacityCalculatorTest {

    // PROMO_START = June 2026, PROMO_END = August 2026
    private static final YearMonth PROMO_START  = YearMonth.of(2026, 6);
    private static final YearMonth PROMO_END    = YearMonth.of(2026, 8);

    // =========================================================================
    // Promo window boundaries
    // =========================================================================

    @Test
    void promoStart_isInsidePromoWindow() {
        // June 2026 — first promo month: business → 3,000/seat
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("business", 1, PROMO_START);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(3_000));
    }

    @Test
    void promoEnd_isInsidePromoWindow() {
        // August 2026 — last promo month: enterprise → 7,000/seat
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("enterprise", 1, PROMO_END);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(7_000));
    }

    @Test
    void monthBeforePromoStart_usesStandardRates() {
        // May 2026 — just before promo: business → 1,900/seat
        YearMonth beforePromo = PROMO_START.minusMonths(1);
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("business", 1, beforePromo);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(1_900));
    }

    @Test
    void monthAfterPromoEnd_usesStandardRates() {
        // September 2026 — just after promo: enterprise → 3,900/seat
        YearMonth afterPromo = PROMO_END.plusMonths(1);
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("enterprise", 1, afterPromo);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(3_900));
    }

    // =========================================================================
    // Standard vs promo credit selection — both plan types
    // =========================================================================

    @Test
    void business_promoRate_multipliedBySeats() {
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("business", 10, PROMO_START);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(30_000));
    }

    @Test
    void business_standardRate_multipliedBySeats() {
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("business", 10, PROMO_END.plusMonths(1));
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(19_000));
    }

    @Test
    void enterprise_promoRate_multipliedBySeats() {
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("enterprise", 5, PROMO_START);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(35_000));
    }

    @Test
    void enterprise_standardRate_multipliedBySeats() {
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity("enterprise", 5, PROMO_END.plusMonths(1));
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(19_500));
    }

    // =========================================================================
    // Case-insensitive planType handling
    // =========================================================================

    @ParameterizedTest
    @ValueSource(strings = {"Business", "BUSINESS", "bUsInEsS"})
    void business_caseInsensitive(String planType) {
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity(planType, 1, PROMO_START);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(3_000));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Enterprise", "ENTERPRISE", "eNtErPrIsE"})
    void enterprise_caseInsensitive(String planType) {
        BigDecimal result = PoolCapacityCalculator.calculatePoolCapacity(planType, 1, PROMO_START);
        assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(7_000));
    }

    // =========================================================================
    // IllegalArgumentException for unknown plan type
    // =========================================================================

    @Test
    void unknownPlanType_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                PoolCapacityCalculator.calculatePoolCapacity("team", 10, PROMO_START))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("team")
                .hasMessageContaining("business")
                .hasMessageContaining("enterprise");
    }

    @Test
    void nullPlanType_throwsIllegalArgumentException() {
        assertThatThrownBy(() ->
                PoolCapacityCalculator.calculatePoolCapacity(null, 10, PROMO_START))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
