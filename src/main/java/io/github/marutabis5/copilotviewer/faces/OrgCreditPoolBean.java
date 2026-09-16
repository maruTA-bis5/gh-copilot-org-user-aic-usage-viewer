package io.github.marutabis5.copilotviewer.faces;

import io.github.marutabis5.copilotviewer.domain.model.OrgCreditPoolOverview;
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
    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                             .withZone(ZoneOffset.UTC);

    @Inject
    CopilotUsageService usageService;

    // ---- Form inputs -------------------------------------------------------
    private YearMonth yearMonth;

    // ---- View state --------------------------------------------------------
    private OrgCreditPoolOverview creditPool;
    private boolean error;

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
            yearMonth = currentUtcMonth();
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

    /** {@code true} when capacity can be calculated from the current seat count. */
    public boolean isCapacityAvailable() {
        return yearMonth != null && yearMonth.equals(currentUtcMonth());
    }

    /**
     * {@code true} when the current-month overview has zero included capacity and no additional budget.
     */
    public boolean isNoData() {
        return creditPool != null
                && isCapacityAvailable()
                && creditPool.getTotalPoolCapacity().signum() == 0
                && !creditPool.isAdditionalBudgetSet();
    }

    YearMonth currentUtcMonth() {
        return YearMonth.now(ZoneOffset.UTC);
    }

    /** {@code true} when the last load attempt produced an error. */
    public boolean isError() {
        return error;
    }

    // =========================================================================
    // Getters / setters
    // =========================================================================

    public YearMonth getYearMonth() { return yearMonth; }
    public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }

    public OrgCreditPoolOverview getCreditPool() { return creditPool; }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private void loadOverview() {
        creditPool = null;
        error = false;

        try {
            creditPool = usageService.getOrgCreditPoolOverview(yearMonth);
        } catch (ValidationException e) {
            error = true;
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Invalid input",
                    e.getMessage()));
        } catch (GitHubApiException e) {
            error = true;
            LOG.warnf("GitHub API error for org credit pool month=%s: HTTP %d – %s",
                    yearMonth, e.getHttpStatus(), e.getSummary());
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "GitHub API Error (HTTP %d)".formatted(e.getHttpStatus()),
                    e.getSummary()));
        } catch (Exception e) {
            error = true;
            LOG.errorf(e, "Unexpected error fetching org credit pool for month=%s", yearMonth);
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_FATAL,
                    "Unexpected error",
                    "An unexpected error occurred. Please contact the administrator."));
        }
    }
}
