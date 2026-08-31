package io.github.marutabis5.copilotviewer.faces;

import io.github.marutabis5.copilotviewer.domain.model.OrgCreditPoolOverview;
import io.github.marutabis5.copilotviewer.service.CopilotUsageService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgCreditPoolBeanTest {

    @Test
    void currentMonth_exposesCapacity() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        OrgCreditPoolBean bean = new OrgCreditPoolBean();
        bean.setYearMonth(currentMonth);

        assertThat(bean.isCapacityAvailable()).isTrue();
    }

    @Test
    void pastMonth_hidesCapacity() {
        YearMonth pastMonth = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
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

        OrgCreditPoolBean bean = new OrgCreditPoolBean();
        bean.usageService = usageService;
        bean.setYearMonth(pastMonth);
        bean.restoreFromParams();

        assertThat(bean.getCreditPool()).isNotNull();
        assertThat(bean.isCapacityAvailable()).isFalse();
        assertThat(bean.isNoData()).isFalse();
    }
}
