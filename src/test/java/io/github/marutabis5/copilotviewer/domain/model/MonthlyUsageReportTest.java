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
}
