package io.github.marutabis5.copilotviewer.faces;

import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.domain.model.DailyUsage;
import io.github.marutabis5.copilotviewer.domain.model.UsageItem;
import io.github.marutabis5.copilotviewer.service.CopilotUsageService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsageBeanTest {

    @Test
    void userBudgetSummaryIsCurrentMonthOnly() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        MonthlyUsageReport currentReport = new MonthlyUsageReport(
                "org", "alice", currentMonth, List.of(), Instant.EPOCH);
        MonthlyUsageReport pastReport = new MonthlyUsageReport(
                "org", "alice", currentMonth.minusMonths(1), List.of(), Instant.EPOCH);

        assertThat(beanWithReport(currentReport, mock(FacesContext.class)).isCurrentMonth()).isTrue();
        assertThat(beanWithReport(pastReport, mock(FacesContext.class)).isCurrentMonth()).isFalse();
    }

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

    @Test
    void unavailableAllocationPreservesUsageAndDisplaysWarning() {
        YearMonth month = YearMonth.of(2026, 11);
        UsageItem item = new UsageItem("Copilot", "AI Credits", "gpt-4o",
                "credits", 100, 0, 100, 1.0);
        MonthlyUsageReport report = new MonthlyUsageReport("org", "alice", month,
                List.of(new DailyUsage(month.atDay(1), List.of(item))), Instant.EPOCH, null, true);
        FacesContext facesContext = mock(FacesContext.class);

        UsageBean bean = beanWithReport(report, facesContext);

        assertThat(bean.getReport()).isSameAs(report);
        assertThat(bean.getReport().getTotalGrossQuantity()).isEqualTo(100);
        assertThat(bean.getReport().isIncludedCreditsUnavailable()).isTrue();
        assertThat(bean.isHasData()).isTrue();
        assertThat(bean.isNoData()).isFalse();
        ArgumentCaptor<FacesMessage> message = ArgumentCaptor.forClass(FacesMessage.class);
        verify(facesContext).addMessage(isNull(), message.capture());
        assertThat(message.getValue().getSeverity()).isEqualTo(FacesMessage.SEVERITY_WARN);
        assertThat(message.getValue().getSummary()).contains("Seat included credits unavailable");
        assertThat(message.getValue().getDetail()).contains("subscription", "permissions");
    }

    @Test
    void unavailableAllocationIsVisibleBeforeFirstUsage() {
        YearMonth month = YearMonth.of(2026, 11);
        MonthlyUsageReport report = new MonthlyUsageReport("org", "alice", month,
                List.of(), Instant.EPOCH, null, true);

        UsageBean bean = beanWithReport(report, mock(FacesContext.class));

        assertThat(bean.isHasData()).isTrue();
        assertThat(bean.isNoData()).isTrue();
        assertThat(bean.getReport().isIncludedCreditsUnavailable()).isTrue();
    }

    private static UsageBean beanWithAllocation(BigDecimal includedCredits) {
        YearMonth month = YearMonth.of(2026, 11);
        MonthlyUsageReport report = new MonthlyUsageReport("org", "alice", month,
                List.of(), Instant.EPOCH, includedCredits);
        return beanWithReport(report, mock(FacesContext.class));
    }

    private static UsageBean beanWithReport(MonthlyUsageReport report, FacesContext facesContext) {
        YearMonth month = report.getYearMonth();
        CopilotUsageService service = mock(CopilotUsageService.class);
        when(service.findUsage("alice", month.toString())).thenReturn(report);
        UsageBean bean = new UsageBean();
        bean.usageService = service;
        bean.setLogin("alice");
        bean.setYearMonth(month);
        try (var context = mockStatic(FacesContext.class)) {
            context.when(FacesContext::getCurrentInstance).thenReturn(facesContext);
            bean.restoreFromParams();
        }
        return bean;
    }
}
