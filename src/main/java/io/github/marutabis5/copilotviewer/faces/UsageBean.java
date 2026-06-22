package io.github.marutabis5.copilotviewer.faces;

import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.service.CopilotUsageService;
import io.github.marutabis5.copilotviewer.service.GitHubApiException;
import io.github.marutabis5.copilotviewer.service.ValidationException;
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
 * JSF backing bean for the Copilot AI credit usage search page.
 */
@Named
@ViewScoped
public class UsageBean implements Serializable {

    private static final Logger LOG = Logger.getLogger(UsageBean.class);
    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")
                             .withZone(ZoneOffset.UTC);

    @Inject
    CopilotUsageService usageService;

    // ---- Form inputs -------------------------------------------------------
    private String login;
    private YearMonth yearMonth;

    // ---- View state --------------------------------------------------------
    private MonthlyUsageReport report;

    // =========================================================================
    // Actions
    // =========================================================================

    /**
     * Triggered by the Search button.
     * Validates inputs, fetches the report, and populates view state.
     * All errors are surfaced through {@link FacesMessage}s so PrimeFaces
     * {@code <p:messages>} can display them.
     */
    public void search() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        report = null;

        try {
            report = usageService.findUsage(login, yearMonth != null ? yearMonth.toString() : null);
        } catch (ValidationException e) {
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Invalid input",
                    e.getMessage()));
        } catch (GitHubApiException e) {
            LOG.warnf("GitHub API error for user=%s month=%s: HTTP %d – %s",
                    login, yearMonth, e.getHttpStatus(), e.getSummary());
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "GitHub API Error (HTTP %d)".formatted(e.getHttpStatus()),
                    e.getSummary()));
        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error for user=%s month=%s", login, yearMonth);
            ctx.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_FATAL,
                    "Unexpected error",
                    "An unexpected error occurred. Please contact the administrator."));
        }
    }

    // =========================================================================
    // View helpers
    // =========================================================================

    /** Formatted fetch time for display in the UI (UTC). */
    public String getFetchedAtDisplay() {
        if (report == null) return "";
        return UTC_FORMATTER.format(report.getFetchedAt());
    }

    /** {@code true} when a successful search returned zero usage items. */
    public boolean isNoData() {
        return report != null && report.isEmpty();
    }

    /** {@code true} when the report has data to display. */
    public boolean isHasData() {
        return report != null && !report.isEmpty();
    }

    // =========================================================================
    // Getters / setters
    // =========================================================================

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public YearMonth getYearMonth() { return yearMonth; }
    public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }

    public MonthlyUsageReport getReport() { return report; }
}
