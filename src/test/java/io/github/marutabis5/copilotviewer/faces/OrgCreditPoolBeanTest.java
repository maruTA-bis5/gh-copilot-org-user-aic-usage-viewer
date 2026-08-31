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
    void currentMonth_exposesCapacity() {
        OrgCreditPoolBean bean = beanWithFixedCurrentMonth();
        bean.setYearMonth(CURRENT_MONTH);

        assertThat(bean.isCapacityAvailable()).isTrue();
    }

    @Test
    void pastMonth_hidesCapacity() {
        YearMonth pastMonth = CURRENT_MONTH.minusMonths(1);
        CopilotUsageService usageService = mock(CopilotUsageService.class);
        when(usageService.getOrgCreditPoolOverview(pastMonth))
                .thenReturn(new OrgCreditPoolOverview(
                        "test-org",
                        pastMonth,
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        Instant.EPOCH));

        OrgCreditPoolBean bean = beanWithFixedCurrentMonth();
        bean.usageService = usageService;
        bean.setYearMonth(pastMonth);
        bean.restoreFromParams();

        assertThat(bean.getCreditPool()).isNotNull();
        assertThat(bean.isCapacityAvailable()).isFalse();
        assertThat(bean.isNoData()).isFalse();
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
