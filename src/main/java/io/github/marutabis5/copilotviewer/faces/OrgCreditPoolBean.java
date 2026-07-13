package io.github.marutabis5.copilotviewer.faces;

import io.github.marutabis5.copilotviewer.domain.model.OrgCreditPoolOverview;
import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.faces.chart.UsageTrendChartJsonBuilder;
import io.github.marutabis5.copilotviewer.service.CopilotUsageService;
import io.github.marutabis5.copilotviewer.service.GitHubApiException;
import io.github.marutabis5.copilotviewer.service.ValidationException;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jboss.logging.Logger;

import java.io.Serializable;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * JSF backing bean for the Organisation Credit Pool Overview page.
 */
@Named
@ViewScoped
public class OrgCreditPoolBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(OrgCreditPoolBean.class);
    private static final UsageTrendChartJsonBuilder CHART_JSON_BUILDER = new UsageTrendChartJsonBuilder();
    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                             .withZone(ZoneOffset.UTC);
    private static final String SERIES_ORG_WIDE = "org-wide";
    private static final String SERIES_MODEL = "model";
    private static final String MODE_PER_DAY = "per-day";
    private static final String MODE_CUMULATIVE = "cumulative";

    @Inject
    CopilotUsageService usageService;

    // ---- Form inputs -------------------------------------------------------
    private YearMonth yearMonth;

    // ---- View state --------------------------------------------------------
    private OrgCreditPoolOverview creditPool;
    private MonthlyUsageReport dailyUsageReport;
    private boolean error;
    private String chartSeries = SERIES_ORG_WIDE;
    private String chartMode = MODE_PER_DAY;

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /** Initializes default view state. */
    @PostConstruct
    public void init() {
        error = false;
    }

    // =========================================================================
    // Actions
    // =========================================================================

    /** Restores state from URL params and loads overview on initial GET. */
    public void restoreFromParams() {
        if (yearMonth == null) {
            yearMonth = YearMonth.now(ZoneOffset.UTC);
        }
        loadOverview();
    }

    /** Triggered by the Search/Refresh button — redirects to bookmarkable URL. */
    public String search() {
        return "org-credit-pool?faces-redirect=true&includeViewParams=true";
    }

    // =========================================================================
    // View helpers
    // =========================================================================

    /** Formatted fetch time for display in the UI (UTC). */
    public String getFetchedAtDisplay() {
        if (creditPool == null) return "";
        return UTC_FORMATTER.format(creditPool.getFetchedAt());
    }

    /** {@code true} when the overview was fetched successfully. */
    public boolean isDataLoaded() {
        return creditPool != null;
    }

    /** {@code true} when the overview loaded but the pool capacity is zero (no seats/plan). */
    public boolean isNoData() {
        return creditPool != null && creditPool.getTotalPoolCapacity().signum() == 0;
    }

    /** {@code true} when the last load attempt produced an error. */
    public boolean isError() {
        return error;
    }

    public String getUsageChartJson() {
        if (dailyUsageReport == null || creditPool == null) {
            return "";
        }
        boolean cumulative = MODE_CUMULATIVE.equals(chartMode);
        if (SERIES_MODEL.equals(chartSeries)) {
            return CHART_JSON_BUILDER.buildPerModelSeries(dailyUsageReport.getDailyUsages(), cumulative);
        }
        return CHART_JSON_BUILDER.buildSingleTotalSeries(
                dailyUsageReport.getDailyUsages(),
                creditPool.getOrg(),
                cumulative);
    }

    // =========================================================================
    // Getters / setters
    // =========================================================================

    public YearMonth getYearMonth() { return yearMonth; }
    public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }

    public OrgCreditPoolOverview getCreditPool() { return creditPool; }

    public String getChartSeries() { return chartSeries; }
    public void setChartSeries(String chartSeries) {
        this.chartSeries = SERIES_MODEL.equals(chartSeries) ? SERIES_MODEL : SERIES_ORG_WIDE;
    }

    public String getChartMode() { return chartMode; }
    public void setChartMode(String chartMode) {
        this.chartMode = MODE_CUMULATIVE.equals(chartMode) ? MODE_CUMULATIVE : MODE_PER_DAY;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void loadOverview() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        creditPool = null;
        dailyUsageReport = null;
        error = false;

        try {
            OrgCreditPoolOverview loadedOverview = usageService.getOrgCreditPoolOverview(yearMonth);
            MonthlyUsageReport loadedDailyUsageReport = usageService.getOrgDailyUsage(yearMonth);
            creditPool = loadedOverview;
            dailyUsageReport = loadedDailyUsageReport;
        } catch (ValidationException e) {
            error = true;
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Invalid input",
                    e.getMessage()));
        } catch (GitHubApiException e) {
            error = true;
            LOG.warnf("GitHub API error for org credit pool month=%s: HTTP %d – %s",
                    yearMonth, e.getHttpStatus(), e.getSummary());
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "GitHub API Error (HTTP %d)".formatted(e.getHttpStatus()),
                    e.getSummary()));
        } catch (Exception e) {
            error = true;
            LOG.errorf(e, "Unexpected error fetching org credit pool for month=%s", yearMonth);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_FATAL,
                    "Unexpected error",
                    "An unexpected error occurred. Please contact the administrator."));
        }
    }
}
