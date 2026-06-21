package io.github.marutabis5.copilotviewer.service;

import io.github.marutabis5.copilotviewer.domain.model.DailyUsage;
import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.domain.model.UsageItem;
import io.github.marutabis5.copilotviewer.infrastructure.github.GitHubApiUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CopilotUsageService}.
 * The CDI injection of {@code GitHubApiUsageRepository} is fulfilled via Mockito;
 * {@code org} is assigned directly (same package, package-private field).
 */
@ExtendWith(MockitoExtension.class)
class CopilotUsageServiceTest {

    @Mock
    GitHubApiUsageRepository apiRepository;

    @InjectMocks
    CopilotUsageService service;

    private static final String TEST_ORG = "test-org";

    @BeforeEach
    void injectOrg() {
        service.org = TEST_ORG;
    }

    // =========================================================================
    // Happy path
    // =========================================================================

    @Test
    void findUsage_returns_report_from_repository() {
        YearMonth ym = YearMonth.of(2025, 1);
        MonthlyUsageReport expected = buildReport("octocat", ym);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym))
                .thenReturn(expected);

        MonthlyUsageReport result = service.findUsage("octocat", "2025-01");

        assertThat(result).isSameAs(expected);
        verify(apiRepository).findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym);
    }

    @Test
    void findUsage_trims_login_whitespace() {
        YearMonth ym = YearMonth.of(2025, 1);
        MonthlyUsageReport expected = buildReport("octocat", ym);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym))
                .thenReturn(expected);

        service.findUsage("  octocat  ", "2025-01");

        verify(apiRepository).findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym);
    }

    @Test
    void findUsage_empty_report_is_returned_without_error() {
        YearMonth ym = YearMonth.of(2025, 1);
        MonthlyUsageReport empty = new MonthlyUsageReport(
                TEST_ORG, "octocat", ym, List.of(), Instant.now());
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym))
                .thenReturn(empty);

        MonthlyUsageReport result = service.findUsage("octocat", "2025-01");

        assertThat(result.isEmpty()).isTrue();
    }

    // =========================================================================
    // Validation failures – repository must never be called
    // =========================================================================

    @Test
    void findUsage_blank_login_throws_ValidationException() {
        assertThatThrownBy(() -> service.findUsage("", "2025-01"))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(apiRepository);
    }

    @Test
    void findUsage_future_month_throws_ValidationException() {
        String futureMonth = YearMonth.now(ZoneOffset.UTC).plusMonths(1).toString();
        assertThatThrownBy(() -> service.findUsage("octocat", futureMonth))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Future");
        verifyNoInteractions(apiRepository);
    }

    @Test
    void findUsage_bad_yearMonth_format_throws_ValidationException() {
        assertThatThrownBy(() -> service.findUsage("octocat", "06-2025"))
                .isInstanceOf(ValidationException.class);
        verifyNoInteractions(apiRepository);
    }

    // =========================================================================
    // Error propagation
    // =========================================================================

    @Test
    void findUsage_propagates_GitHubApiException() {
        YearMonth ym = YearMonth.of(2025, 1);
        when(apiRepository.findByOrgAndUserAndMonth(anyString(), anyString(), any()))
                .thenThrow(new GitHubApiException(403, "Forbidden"));

        assertThatThrownBy(() -> service.findUsage("octocat", "2025-01"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus())
                .isEqualTo(403);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static MonthlyUsageReport buildReport(String login, YearMonth ym) {
        UsageItem item = new UsageItem("Copilot", "AI Credits", "gpt-4o",
                "credits", 100, 0, 100, 1.0);
        DailyUsage day = new DailyUsage(ym.atDay(1), List.of(item));
        return new MonthlyUsageReport(TEST_ORG, login, ym, List.of(day), Instant.now());
    }
}
