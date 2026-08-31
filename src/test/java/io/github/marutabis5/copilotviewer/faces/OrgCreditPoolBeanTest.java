package io.github.marutabis5.copilotviewer.faces;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

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
        OrgCreditPoolBean bean = new OrgCreditPoolBean();
        bean.setYearMonth(pastMonth);

        assertThat(bean.isCapacityAvailable()).isFalse();
        assertThat(bean.isNoData()).isFalse();
    }
}
