package io.github.marutabis5.copilotviewer.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonthlyUsageReportTest {

    @Test
    void unavailableAllocationIsDistinctFromAnUncalculatedAllocation() {
        YearMonth month = YearMonth.of(2026, 11);
        MonthlyUsageReport uncalculated = new MonthlyUsageReport(
                "org", "alice", month, List.of(), Instant.EPOCH);
        MonthlyUsageReport unavailable = new MonthlyUsageReport(
                "org", "alice", month, List.of(), Instant.EPOCH, null, true);

        assertThat(uncalculated.isIncludedCreditsUnavailable()).isFalse();
        assertThat(unavailable.isIncludedCreditsUnavailable()).isTrue();
        assertThat(unavailable).isNotEqualTo(uncalculated);
    }

    @Test
    void unavailableAllocationCannotHaveACreditValue() {
        assertThatThrownBy(() -> new MonthlyUsageReport(
                "org", "alice", YearMonth.of(2026, 11), List.of(), Instant.EPOCH,
                BigDecimal.ZERO, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userBudgetMetricsCountIncludedGrossCreditsAndCapAtTheBudget() {
        YearMonth month = YearMonth.of(2026, 11);
        DailyUsage day = new DailyUsage(month.atDay(1), List.of(
                new UsageItem("Copilot", "AI Credits", "model", "credits",
                        20, 0, 12, 1)));

        MonthlyUsageReport report = new MonthlyUsageReport(
                "org", "alice", month, List.of(day), Instant.EPOCH,
                null, false, BigDecimal.valueOf(25));

        assertThat(report.isUserBudgetSet()).isTrue();
        assertThat(report.getUserBudgetCapacityCredits()).isEqualByComparingTo("25");
        assertThat(report.getRemainingUserBudgetCredits()).isEqualByComparingTo("5");
        assertThat(report.getUserBudgetUsageRatePercent()).isEqualByComparingTo("80");
        assertThat(report.isUserBudgetBlocked()).isFalse();
    }

    @Test
    void zeroCapacityBudgetIsExplicitlyBlockedAndShowsFullyUsedProgress() {
        MonthlyUsageReport report = new MonthlyUsageReport(
                "org", "alice", YearMonth.of(2026, 11), List.of(), Instant.EPOCH,
                null, false, BigDecimal.ZERO, null, false, false, true);

        assertThat(report.getRemainingUserBudgetCredits()).isZero();
        assertThat(report.getUserBudgetUsageRatePercent()).isEqualByComparingTo("100");
        assertThat(report.isUserBudgetBlocked()).isTrue();
    }

    @Test
    void exhaustedBudgetDoesNotBlockWhenFurtherUsageIsAllowed() {
        MonthlyUsageReport report = new MonthlyUsageReport(
                "org", "alice", YearMonth.of(2026, 11), List.of(), Instant.EPOCH,
                null, false, BigDecimal.TEN, BigDecimal.TEN, false, false, false);

        assertThat(report.getUserBudgetUsageRatePercent()).isEqualByComparingTo("100");
        assertThat(report.isUserBudgetBlocked()).isFalse();
    }

    @Test
    void userBudgetMetricsAreUnavailableWhenNoBudgetIsSet() {
        MonthlyUsageReport report = new MonthlyUsageReport(
                "org", "alice", YearMonth.of(2026, 11), List.of(), Instant.EPOCH);

        assertThat(report.isUserBudgetSet()).isFalse();
        assertThat(report.getUserBudgetCapacityCredits()).isNull();
        assertThat(report.getRemainingUserBudgetCredits()).isNull();
        assertThat(report.getUserBudgetUsageRatePercent()).isNull();
    }

    @Test
    void universalBudgetUsesApiReportedPerUserConsumption() {
        MonthlyUsageReport report = new MonthlyUsageReport(
                "org", "alice", YearMonth.of(2026, 11), List.of(), Instant.EPOCH,
                null, false, BigDecimal.valueOf(1000), BigDecimal.valueOf(450), true, false, true);

        assertThat(report.isUniversalBudget()).isTrue();
        assertThat(report.getRemainingUserBudgetCredits()).isEqualByComparingTo("550");
        assertThat(report.getUserBudgetUsageRatePercent()).isEqualByComparingTo("45");
        assertThat(report.isUserBudgetBlocked()).isFalse();
    }

    @Test
    void unavailableBudgetLookupIsDistinctFromNoBudget() {
        MonthlyUsageReport unavailable = new MonthlyUsageReport(
                "org", "alice", YearMonth.of(2026, 11), List.of(), Instant.EPOCH,
                null, false, null, null, false, true);

        assertThat(unavailable.isUserBudgetLookupUnavailable()).isTrue();
        assertThat(unavailable.isUserBudgetSet()).isFalse();
    }
}
