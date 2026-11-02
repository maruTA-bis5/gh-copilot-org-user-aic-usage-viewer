package io.github.marutabis5.copilotviewer.service;

import io.github.marutabis5.copilotviewer.domain.model.CopilotBillingInfo;
import io.github.marutabis5.copilotviewer.domain.model.DailyUsage;
import io.github.marutabis5.copilotviewer.domain.model.MonthlyUsageReport;
import io.github.marutabis5.copilotviewer.domain.model.OrgCreditPoolOverview;
import io.github.marutabis5.copilotviewer.domain.model.UsageItem;
import io.github.marutabis5.copilotviewer.infrastructure.github.GitHubApiUsageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Optional;
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

    @Test
    void getOrgCreditPoolOverview_preserves_budget_information() {
        YearMonth ym = YearMonth.of(2026, 9);
        when(apiRepository.findCopilotBillingInfo(TEST_ORG))
                .thenReturn(Optional.of(new CopilotBillingInfo(2, "business")));
        when(apiRepository.findCopilotSeatAssignmentDates(TEST_ORG))
                .thenReturn(List.of(ym.atDay(1), ym.minusMonths(1).atDay(10)));
        when(apiRepository.findOrgCreditPoolUsage(TEST_ORG, ym))
                .thenReturn(new OrgCreditPoolOverview(
                        TEST_ORG,
                        ym,
                        BigDecimal.valueOf(30),
                        BigDecimal.valueOf(20),
                        BigDecimal.valueOf(12),
                        BigDecimal.valueOf(6),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(1000),
                        true,
                        Instant.EPOCH));

        OrgCreditPoolOverview result = service.getOrgCreditPoolOverview(ym);

        assertThat(result.getTotalPoolCapacity()).isEqualByComparingTo("3800");
        assertThat(result.getAdditionalBudgetCredits()).isEqualByComparingTo("1000");
        assertThat(result.isPreventFurtherUsage()).isTrue();
        assertThat(result.getAdditionalCreditsUsedWithinBudget()).isEqualByComparingTo("12");
        assertThat(result.getRemainingAdditionalCredits()).isEqualByComparingTo("988");
        assertThat(result.getCreditBudgetOverage()).isEqualByComparingTo("0");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    @Test
    void getOrgCreditPoolOverview_sumsProratedCreditsPerSeat() {
        YearMonth ym = YearMonth.of(2026, 11);
        when(apiRepository.findCopilotBillingInfo(TEST_ORG))
                .thenReturn(Optional.of(new CopilotBillingInfo(3, "business")));
        when(apiRepository.findCopilotSeatAssignmentDates(TEST_ORG))
                .thenReturn(List.of(ym.atDay(1), ym.atDay(2), ym.atDay(2)));
        when(apiRepository.findOrgCreditPoolUsage(TEST_ORG, ym))
                .thenReturn(emptyOverview(ym));

        assertThat(service.getOrgCreditPoolOverview(ym).getTotalPoolCapacity())
                .isEqualByComparingTo("5572");
    }

    @Test
    void findUsage_returnsAllocationForQueriedUserEvenWithoutUsage() {
        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "new-user", ym))
                .thenReturn(new MonthlyUsageReport(TEST_ORG, "new-user", ym, List.of(), Instant.EPOCH));
        when(apiRepository.findCopilotBillingInfo(TEST_ORG))
                .thenReturn(Optional.of(new CopilotBillingInfo(2, "business")));
        when(apiRepository.findCopilotSeatAssignmentDate(TEST_ORG, "new-user"))
                .thenReturn(Optional.of(ym.atDay(2)));

        MonthlyUsageReport result = service.findUsage("new-user", ym.toString());

        assertThat(result.getIncludedCredits()).isEqualByComparingTo(
                PoolCapacityCalculator.calculateSeatCredits("business", ym.atDay(2), ym));
        assertThat(result.getTotalDiscountQuantity()).isZero();
        assertThat(result.isEmpty()).isTrue();
        verify(apiRepository, never()).findCopilotSeatAssignmentDates(anyString());
    }

    @Test
    void findUsage_existingUserReceivesFullAllocation() {
        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "existing", ym))
                .thenReturn(buildReport("existing", ym));
        when(apiRepository.findCopilotBillingInfo(TEST_ORG))
                .thenReturn(Optional.of(new CopilotBillingInfo(2, "business")));
        when(apiRepository.findCopilotSeatAssignmentDate(TEST_ORG, "existing"))
                .thenReturn(Optional.of(ym.minusMonths(1).atDay(1)));

        assertThat(service.findUsage("existing", ym.toString()).getIncludedCredits())
                .isEqualByComparingTo(PoolCapacityCalculator.calculatePoolCapacity("business", 1, ym));
    }

    @Test
    void findUsage_withoutSeatHasNoAllocation() {
        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "no-seat", ym))
                .thenReturn(buildReport("no-seat", ym));
        when(apiRepository.findCopilotBillingInfo(TEST_ORG))
                .thenReturn(Optional.of(new CopilotBillingInfo(0, "business")));
        when(apiRepository.findCopilotSeatAssignmentDate(TEST_ORG, "no-seat"))
                .thenReturn(Optional.empty());

        MonthlyUsageReport report = service.findUsage("no-seat", ym.toString());

        assertThat(report.getIncludedCredits()).isZero();
        assertThat(report.isIncludedCreditsUnavailable()).isFalse();
    }

    @Test
    void findUsage_withoutBillingInfoPreservesUsageAndMarksAllocationUnavailable() {
        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        MonthlyUsageReport original = buildReport("octocat", ym);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym))
                .thenReturn(original);
        when(apiRepository.findCopilotBillingInfo(TEST_ORG)).thenReturn(Optional.empty());

        MonthlyUsageReport report = service.findUsage("octocat", ym.toString());

        assertThat(report.getIncludedCredits()).isNull();
        assertThat(report.isIncludedCreditsUnavailable()).isTrue();
        assertThat(report.getOrg()).isEqualTo(original.getOrg());
        assertThat(report.getLogin()).isEqualTo(original.getLogin());
        assertThat(report.getYearMonth()).isEqualTo(original.getYearMonth());
        assertThat(report.getFetchedAt()).isEqualTo(original.getFetchedAt());
        assertThat(report.getDailyUsages()).isEqualTo(original.getDailyUsages());
        assertThat(report.getTotalGrossQuantity()).isEqualTo(100);
        verify(apiRepository, never()).findCopilotSeatAssignmentDate(anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 429, 500})
    void findUsage_propagatesOtherBillingErrors(int status) {
        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym))
                .thenReturn(buildReport("octocat", ym));
        GitHubApiException failure = new GitHubApiException(status, "Billing API failed");
        when(apiRepository.findCopilotBillingInfo(TEST_ORG)).thenThrow(failure);

        assertThatThrownBy(() -> service.findUsage("octocat", ym.toString())).isSameAs(failure);
        verify(apiRepository, never()).findCopilotSeatAssignmentDate(anyString(), anyString());
    }

    @Test
    void findUsage_propagatesUnrecognizedPlanType() {
        YearMonth ym = YearMonth.now(ZoneOffset.UTC);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym))
                .thenReturn(buildReport("octocat", ym));
        when(apiRepository.findCopilotBillingInfo(TEST_ORG))
                .thenReturn(Optional.of(new CopilotBillingInfo(1, "unknown")));

        assertThatThrownBy(() -> service.findUsage("octocat", ym.toString()))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus()).isEqualTo(422);
        verify(apiRepository, never()).findCopilotSeatAssignmentDate(anyString(), anyString());
    }

    @Test
    void findUsage_doesNotEstimatePastAllocationFromCurrentSeats() {
        YearMonth ym = YearMonth.now(ZoneOffset.UTC).minusMonths(1);
        when(apiRepository.findByOrgAndUserAndMonth(TEST_ORG, "octocat", ym))
                .thenReturn(buildReport("octocat", ym));

        MonthlyUsageReport report = service.findUsage("octocat", ym.toString());

        assertThat(report.getIncludedCredits()).isNull();
        assertThat(report.isIncludedCreditsUnavailable()).isFalse();
        verify(apiRepository, never()).findCopilotBillingInfo(anyString());
        verify(apiRepository, never()).findCopilotSeatAssignmentDate(anyString(), anyString());
    }

    @Test
    void getOrgCreditPoolOverview_withoutBillingInfoStillFails() {
        YearMonth ym = YearMonth.of(2026, 9);
        when(apiRepository.findCopilotBillingInfo(TEST_ORG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrgCreditPoolOverview(ym))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus()).isEqualTo(404);
        verify(apiRepository, never()).findCopilotSeatAssignmentDates(anyString());
        verify(apiRepository, never()).findOrgCreditPoolUsage(anyString(), any());
    }

    @Test
    void getOrgCreditPoolOverview_propagatesSeatApiFailure() {
        YearMonth ym = YearMonth.of(2026, 9);
        when(apiRepository.findCopilotBillingInfo(TEST_ORG))
                .thenReturn(Optional.of(new CopilotBillingInfo(2, "business")));
        when(apiRepository.findCopilotSeatAssignmentDates(TEST_ORG))
                .thenThrow(new GitHubApiException(403, "Forbidden"));

        assertThatThrownBy(() -> service.getOrgCreditPoolOverview(ym))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus()).isEqualTo(403);
    }

    private static OrgCreditPoolOverview emptyOverview(YearMonth ym) {
        return new OrgCreditPoolOverview(TEST_ORG, ym, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, Instant.EPOCH);
    }

    private static MonthlyUsageReport buildReport(String login, YearMonth ym) {
        UsageItem item = new UsageItem("Copilot", "AI Credits", "gpt-4o",
                "credits", 100, 0, 100, 1.0);
        DailyUsage day = new DailyUsage(ym.atDay(1), List.of(item));
        return new MonthlyUsageReport(TEST_ORG, login, ym, List.of(day), Instant.now());
    }
}
