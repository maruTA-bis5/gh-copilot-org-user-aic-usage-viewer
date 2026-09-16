package io.github.marutabis5.copilotviewer.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class OrgCreditPoolOverviewTest {

    @Test
    void derives_additional_budget_values_from_credit_quantity() {
        OrgCreditPoolOverview overview = new OrgCreditPoolOverview(
                "org",
                YearMonth.of(2026, 9),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(14),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                false,
                Instant.EPOCH);

        assertThat(overview.isAdditionalBudgetSet()).isTrue();
        assertThat(overview.getAdditionalCreditsUsedWithinBudget()).isEqualByComparingTo("8");
        assertThat(overview.getRemainingAdditionalCredits()).isEqualByComparingTo("2");
        assertThat(overview.getCreditBudgetOverage()).isEqualByComparingTo("0");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("80");
        assertThat(overview.isCreditBudgetOverageVisible()).isFalse();
    }

    @Test
    void hides_budget_details_when_budget_is_not_set() {
        OrgCreditPoolOverview overview = new OrgCreditPoolOverview(
                "org",
                YearMonth.of(2026, 9),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(6),
                BigDecimal.valueOf(100),
                null,
                false,
                Instant.EPOCH);

        assertThat(overview.isAdditionalBudgetSet()).isFalse();
        assertThat(overview.getAdditionalCreditsUsedWithinBudget()).isEqualByComparingTo("0");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("0");
        assertThat(overview.getRemainingAdditionalCredits()).isEqualByComparingTo("0");
        assertThat(overview.getCreditBudgetOverage()).isEqualByComparingTo("0");
        assertThat(overview.isCreditBudgetOverageVisible()).isFalse();
    }

    @Test
    void derives_budget_overage_from_total_net_quantity() {
        OrgCreditPoolOverview overview = new OrgCreditPoolOverview(
                "org",
                YearMonth.of(2026, 9),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(14),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                false,
                Instant.EPOCH);

        assertThat(overview.getAdditionalCreditsUsedWithinBudget()).isEqualByComparingTo("10");
        assertThat(overview.getRemainingAdditionalCredits()).isEqualByComparingTo("0");
        assertThat(overview.getCreditBudgetOverage()).isEqualByComparingTo("4");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("100");
        assertThat(overview.isCreditBudgetOverageVisible()).isTrue();
    }
}
