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
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(12),
                BigDecimal.valueOf(6),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                false,
                Instant.EPOCH);

        assertThat(overview.isAdditionalBudgetSet()).isTrue();
        assertThat(overview.getAdditionalBudgetUsedAmount()).isEqualByComparingTo("6");
        assertThat(overview.getRemainingAdditionalBudgetAmount()).isEqualByComparingTo("4");
        assertThat(overview.getAdditionalBudgetOverageAmount()).isEqualByComparingTo("0");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("60");
        assertThat(overview.isAdditionalBudgetOverageVisible()).isFalse();
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
        assertThat(overview.getAdditionalBudgetUsedAmount()).isEqualByComparingTo("0");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("0");
        assertThat(overview.getRemainingAdditionalBudgetAmount()).isEqualByComparingTo("0");
        assertThat(overview.getAdditionalBudgetOverageAmount()).isEqualByComparingTo("0");
        assertThat(overview.isAdditionalBudgetOverageVisible()).isFalse();
    }

    @Test
    void hides_overage_section_when_additional_usage_stays_within_budget() {
        OrgCreditPoolOverview overview = new OrgCreditPoolOverview(
                "org",
                YearMonth.of(2026, 9),
                BigDecimal.valueOf(30),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(8),
                BigDecimal.valueOf(4),
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(10),
                false,
                Instant.EPOCH);

        assertThat(overview.getAdditionalBudgetOverageAmount()).isEqualByComparingTo("0");
        assertThat(overview.isAdditionalBudgetOverageVisible()).isFalse();
    }

    @Test
    void derives_budget_overage_from_total_net_amount() {
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

        assertThat(overview.getAdditionalBudgetUsedAmount()).isEqualByComparingTo("10");
        assertThat(overview.getRemainingAdditionalBudgetAmount()).isEqualByComparingTo("0");
        assertThat(overview.getAdditionalBudgetOverageAmount()).isEqualByComparingTo("4");
        assertThat(overview.getAdditionalBudgetUsageRatePercent()).isEqualByComparingTo("100");
        assertThat(overview.isAdditionalBudgetOverageVisible()).isTrue();
    }
}
