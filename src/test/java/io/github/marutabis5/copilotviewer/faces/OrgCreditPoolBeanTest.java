package io.github.marutabis5.copilotviewer.faces;

import io.github.marutabis5.copilotviewer.domain.model.OrgCreditPoolOverview;
import io.github.marutabis5.copilotviewer.service.CopilotUsageService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgCreditPoolBeanTest {

    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 8);

    @Test
    void missingMonth_defaultsToCurrentMonthAndExposesCapacity() {
        CopilotUsageService usageService = mock(CopilotUsageService.class);
        when(usageService.getOrgCreditPoolOverview(CURRENT_MONTH))
                .thenReturn(overview(CURRENT_MONTH, BigDecimal.TEN));

        OrgCreditPoolBean bean = beanWithFixedCurrentMonth();
        bean.usageService = usageService;
        bean.restoreFromParams();

        assertThat(bean.getYearMonth()).isEqualTo(CURRENT_MONTH);
        assertThat(bean.isCapacityAvailable()).isTrue();
    }

    @Test
    void pastMonth_hidesCapacity() {
        YearMonth pastMonth = CURRENT_MONTH.minusMonths(1);
        CopilotUsageService usageService = mock(CopilotUsageService.class);
        when(usageService.getOrgCreditPoolOverview(pastMonth))
                .thenReturn(overview(pastMonth, BigDecimal.ZERO));

        OrgCreditPoolBean bean = beanWithFixedCurrentMonth();
        bean.usageService = usageService;
        bean.setYearMonth(pastMonth);
        bean.restoreFromParams();

        assertThat(bean.getCreditPool()).isNotNull();
        assertThat(bean.isCapacityAvailable()).isFalse();
        assertThat(bean.isNoData()).isFalse();
    }

    @Test
    void currentMonth_with_additional_budget_is_not_treated_as_noData() {
        CopilotUsageService usageService = mock(CopilotUsageService.class);
        when(usageService.getOrgCreditPoolOverview(CURRENT_MONTH))
                .thenReturn(overview(CURRENT_MONTH, BigDecimal.ZERO, new BigDecimal("10"), false));

        OrgCreditPoolBean bean = beanWithFixedCurrentMonth();
        bean.usageService = usageService;
        bean.restoreFromParams();

        assertThat(bean.isNoData()).isFalse();
    }

    private static OrgCreditPoolOverview overview(YearMonth yearMonth, BigDecimal capacity) {
        return overview(yearMonth, capacity, null, false);
    }

    private static OrgCreditPoolOverview overview(YearMonth yearMonth, BigDecimal capacity,
                                                  BigDecimal additionalBudget,
                                                  boolean preventFurtherUsage) {
        return new OrgCreditPoolOverview(
                "test-org",
                yearMonth,
                BigDecimal.TEN,
                BigDecimal.ONE,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                capacity,
                additionalBudget,
                preventFurtherUsage,
                Instant.EPOCH);
    }

    private static OrgCreditPoolBean beanWithFixedCurrentMonth() {
        return new OrgCreditPoolBean() {
            @Override
            YearMonth currentUtcMonth() {
                return CURRENT_MONTH;
            }
        };
    }
}
