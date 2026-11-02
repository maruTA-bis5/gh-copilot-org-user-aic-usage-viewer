package io.github.marutabis5.copilotviewer.faces;

import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.service.CopilotUsageService;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class UsageBeanTest {

    @Test
    void allocationIsVisibleBeforeFirstUsage() {
        UsageBean bean = beanWithAllocation(BigDecimal.valueOf(1836));

        assertThat(bean.isHasData()).isTrue();
        assertThat(bean.isNoData()).isTrue();
        assertThat(bean.getReport().getIncludedCredits()).isEqualByComparingTo("1836");
    }

    @Test
    void emptyHistoricalReportStillShowsOnlyNoDataMessage() {
        UsageBean bean = beanWithAllocation(null);

        assertThat(bean.isHasData()).isFalse();
        assertThat(bean.isNoData()).isTrue();
    }

    private static UsageBean beanWithAllocation(BigDecimal includedCredits) {
        YearMonth month = YearMonth.of(2026, 11);
        CopilotUsageService service = mock(CopilotUsageService.class);
        when(service.findUsage("alice", month.toString()))
                .thenReturn(new MonthlyUsageReport("org", "alice", month,
                        List.of(), Instant.EPOCH, includedCredits));
        UsageBean bean = new UsageBean();
        bean.usageService = service;
        bean.setLogin("alice");
        bean.setYearMonth(month);
        try (var context = mockStatic(FacesContext.class)) {
            context.when(FacesContext::getCurrentInstance).thenReturn(mock(FacesContext.class));
            bean.restoreFromParams();
        }
        return bean;
    }
}
