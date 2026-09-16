package io.github.marutabis5.copilotviewer.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class OrgCreditPoolOverviewTest {

    @Test
    void derives_additional_budget_values_and_overage_visibility() {
        OrgCreditPoolOverview overview = new OrgCreditPoolOverview(
                "org",
                YearMonth.of(2026, 9),
                new BigDecimal("30"),
                new BigDecimal("20"),
                new BigDecimal("12"),
                new BigDecimal("6"),
                new BigDecimal("100"),
                new BigDecimal("10"),
                false,
                Instant.EPOCH);

        assertThat(overview.isAdditionalCreditBudgetSet()).isTrue();
        assertThat(overview.getAdditionalCreditsUsedWithinBudget()).isEqualByComparingTo("10");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("100");
        assertThat(overview.getRemainingAdditionalCredits()).isEqualByComparingTo("0");
        assertThat(overview.getCreditBudgetOverage()).isEqualByComparingTo("2");
        assertThat(overview.isCreditBudgetOverageVisible()).isTrue();
    }

    @Test
    void hides_budget_details_when_budget_is_not_set() {
        OrgCreditPoolOverview overview = new OrgCreditPoolOverview(
                "org",
                YearMonth.of(2026, 9),
                new BigDecimal("30"),
                new BigDecimal("20"),
                new BigDecimal("12"),
                new BigDecimal("6"),
                new BigDecimal("100"),
                null,
                false,
                Instant.EPOCH);

        assertThat(overview.isAdditionalCreditBudgetSet()).isFalse();
        assertThat(overview.getAdditionalCreditsUsedWithinBudget()).isEqualByComparingTo("0");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("0");
        assertThat(overview.getRemainingAdditionalCredits()).isEqualByComparingTo("0");
        assertThat(overview.getCreditBudgetOverage()).isEqualByComparingTo("0");
        assertThat(overview.isCreditBudgetOverageVisible()).isFalse();
    }

    @Test
    void hides_overage_section_when_additional_usage_stays_within_budget() {
        OrgCreditPoolOverview overview = new OrgCreditPoolOverview(
                "org",
                YearMonth.of(2026, 9),
                new BigDecimal("30"),
                new BigDecimal("20"),
                new BigDecimal("8"),
                new BigDecimal("6"),
                new BigDecimal("100"),
                new BigDecimal("10"),
                false,
                Instant.EPOCH);

        assertThat(overview.getCreditBudgetOverage()).isEqualByComparingTo("0");
        assertThat(overview.isCreditBudgetOverageVisible()).isFalse();
    }
}
