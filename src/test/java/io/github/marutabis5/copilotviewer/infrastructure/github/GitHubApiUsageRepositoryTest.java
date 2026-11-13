package io.github.marutabis5.copilotviewer.infrastructure.github;

import io.github.marutabis5.copilotviewer.infrastructure.github.dto.AiCreditUsageResponse;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.BudgetDto;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.BudgetsResponse;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.CopilotBillingResponse;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.CopilotSeatDto;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.CopilotSeatsResponse;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.SeatBreakdownDto;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.UsageItemDto;
import io.github.marutabis5.copilotviewer.service.GitHubApiException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GitHubApiUsageRepository}.
 *
 * <p>Retry behaviour is provided by the MicroProfile Fault Tolerance
 * {@code @Retry} interceptor and is therefore not exercised here (no CDI
 * container in unit tests).  These tests focus on exception routing
 * (retryable vs. non-retryable status codes) and response mapping.</p>
 */
@ExtendWith(MockitoExtension.class)
class GitHubApiUsageRepositoryTest {

    @Mock
    io.github.marutabis5.copilotviewer.infrastructure.github.GitHubBillingClient billingClient;

    @InjectMocks
    GitHubApiUsageRepository repository;

    @BeforeEach
    void setUp() {
        // Self-reference needed for CDI proxy invocation in production;
        // in unit tests we point self at the plain instance (no @Retry applied).
        repository.self = repository;
    }

    // =========================================================================
    // fetchDayWithRetry – success path
    // =========================================================================

    @Test
    void fetchDay_success_on_first_attempt() {
        AiCreditUsageResponse resp = buildResponse(50, 5.0);
        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenReturn(resp);

        AiCreditUsageResponse result =
                repository.fetchDayWithRetry("myorg", "alice", 2025, 6, 15);

        assertThat(result).isSameAs(resp);
        verify(billingClient, times(1))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    // =========================================================================
    // fetchDayWithRetry – exception routing
    // =========================================================================

    @Test
    void fetchDay_rethrows_WebApplicationException_on_429() {
        // Retryable statuses rethrow WebApplicationException so that
        // the @Retry interceptor can retry the call in production.
        WebApplicationException ex = webAppException(429);
        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex);

        assertThatThrownBy(() -> repository.fetchDayWithRetry("org", "user", 2025, 1, 1))
                .isInstanceOf(WebApplicationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 501, 502, 503, 505})
    void fetchDay_rethrows_WebApplicationException_on_5xx(int status) {
        WebApplicationException ex = webAppException(status);
        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex);

        assertThatThrownBy(() -> repository.fetchDayWithRetry("org", "user", 2025, 1, 1))
                .isInstanceOf(WebApplicationException.class);
    }

    @Test
    void fetchDay_throws_GitHubApiException_on_403() {
        WebApplicationException ex = webAppException(403);
        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex);

        assertThatThrownBy(() -> repository.fetchDayWithRetry("org", "user", 2025, 1, 1))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus())
                .isEqualTo(403);

        verify(billingClient, times(1))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void fetchDay_throws_GitHubApiException_on_404() {
        WebApplicationException ex = webAppException(404);
        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex);

        assertThatThrownBy(() -> repository.fetchDayWithRetry("org", "user", 2025, 1, 1))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus())
                .isEqualTo(404);

        verify(billingClient, times(1))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    // =========================================================================
    // findByOrgAndUserAndMonth – month boundary and error propagation
    // =========================================================================

    @Test
    void findByOrgAndUserAndMonth_wraps_exhausted_retry_as_GitHubApiException() {
        // After @Retry exhausts retries, WebApplicationException propagates to
        // findByOrgAndUserAndMonth, which must wrap it as GitHubApiException.
        // In unit tests (no CDI), self.fetchDayWithRetry() == repository.fetchDayWithRetry(),
        // so a retryable status rethrows WebApplicationException immediately.
        WebApplicationException ex = webAppException(429);
        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex);

        assertThatThrownBy(() -> repository.findByOrgAndUserAndMonth("org", "user", YearMonth.of(2025, 1)))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus())
                .isEqualTo(429);
    }

    @Test
    void findByOrgAndUserAndMonth_skips_days_with_empty_usageItems() {
        AiCreditUsageResponse emptyResp = new AiCreditUsageResponse();

        // Feb 2025 has 28 days
        YearMonth feb2025 = YearMonth.of(2025, 2);
        when(billingClient.getAiCreditUsage(eq("org"), eq(2025), eq(2), anyInt(), eq("bob")))
                .thenReturn(emptyResp);

        var report = repository.findByOrgAndUserAndMonth("org", "bob", feb2025);

        assertThat(report.isEmpty()).isTrue();
        assertThat(report.getDailyUsages()).isEmpty();
        verify(billingClient, times(28))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void findByOrgAndUserAndMonth_only_includes_days_with_data() {
        AiCreditUsageResponse emptyResp = new AiCreditUsageResponse();
        AiCreditUsageResponse dataResp = buildResponse(10, 1.0);

        YearMonth feb2025 = YearMonth.of(2025, 2);
        // Return data only for day 14
        when(billingClient.getAiCreditUsage(eq("org"), eq(2025), eq(2), anyInt(), eq("carol")))
                .thenReturn(emptyResp);
        when(billingClient.getAiCreditUsage("org", 2025, 2, 14, "carol"))
                .thenReturn(dataResp);

        var report = repository.findByOrgAndUserAndMonth("org", "carol", feb2025);

        assertThat(report.getDailyUsages()).hasSize(1);
        assertThat(report.getDailyUsages().get(0).getDate().getDayOfMonth()).isEqualTo(14);
    }

    @Test
    void findOrgCreditPoolUsage_maps_matching_organization_ai_credit_budget() {
        when(billingClient.getAiCreditUsage("org", 2026, 9, null))
                .thenReturn(buildResponse(12, 6.0));
        when(billingClient.getBudgets("org", 100, 1, "organization", null))
                .thenReturn(buildBudgetsResponse(matchingBudget(10, true), repositoryBudget()));

        var result = repository.findOrgCreditPoolUsage("org", YearMonth.of(2026, 9));

        assertThat(result.isPoolCapacityAvailable()).isFalse();
        assertThat(result.getTotalPoolCapacity()).isNull();
        assertThat(result.getRemainingPool()).isNull();
        assertThat(result.getUsageRatePercent()).isNull();
        assertThat(result.getTotalNetQuantity()).isEqualByComparingTo("12.0");
        assertThat(result.getAdditionalBudgetCredits()).isEqualByComparingTo("1000.0");
        assertThat(result.getAdditionalCreditsUsedWithinBudget()).isEqualByComparingTo("12.0");
        assertThat(result.getRemainingAdditionalCredits()).isEqualByComparingTo("988.0");
        assertThat(result.isPreventFurtherUsage()).isTrue();
    }

    @Test
    void findOrgCreditPoolUsage_leaves_budget_unset_when_no_matching_org_ai_credit_budget_exists() {
        when(billingClient.getAiCreditUsage("org", 2026, 9, null))
                .thenReturn(buildResponse(12, 6.0));
        when(billingClient.getBudgets("org", 100, 1, "organization", null))
                .thenReturn(buildBudgetsResponse(repositoryBudget()));

        var result = repository.findOrgCreditPoolUsage("org", YearMonth.of(2026, 9));

        assertThat(result.isAdditionalBudgetSet()).isFalse();
        assertThat(result.getAdditionalBudgetCredits()).isNull();
        assertThat(result.isCreditBudgetOverageVisible()).isFalse();
    }

    @Test
    void findOrgCreditPoolUsage_follows_budget_pagination_until_match_is_found() {
        when(billingClient.getAiCreditUsage("org", 2026, 9, null))
                .thenReturn(buildResponse(12, 6.0));
        when(billingClient.getBudgets("org", 100, 1, "organization", null))
                .thenReturn(buildBudgetsResponse(true, repositoryBudget()));
        when(billingClient.getBudgets("org", 100, 2, "organization", null))
                .thenReturn(buildBudgetsResponse(false, matchingBudget(15, false)));

        var result = repository.findOrgCreditPoolUsage("org", YearMonth.of(2026, 9));

        assertThat(result.getAdditionalBudgetCredits()).isEqualByComparingTo("1500.0");
        assertThat(result.isPreventFurtherUsage()).isFalse();
        verify(billingClient).getBudgets("org", 100, 1, "organization", null);
        verify(billingClient).getBudgets("org", 100, 2, "organization", null);
    }

    @Test
    void findByOrgAndUserAndMonth_maps_matching_current_user_ai_credit_budget() {
        YearMonth currentMonth = YearMonth.now(java.time.ZoneOffset.UTC);
        AiCreditUsageResponse emptyResponse = new AiCreditUsageResponse();
        UsageItemDto usage = new UsageItemDto();
        usage.setProduct("Copilot");
        usage.setSku("AI Credits");
        usage.setModel("gpt-4o");
        usage.setUnitType("credits");
        usage.setGrossQuantity(12);
        usage.setDiscountQuantity(12);
        usage.setNetQuantity(0);
        usage.setNetAmount(1.0);
        AiCreditUsageResponse usageResponse = new AiCreditUsageResponse();
        usageResponse.setUsageItems(List.of(usage));
        when(billingClient.getAiCreditUsage(eq("org"), eq(currentMonth.getYear()),
                eq(currentMonth.getMonthValue()), anyInt(), eq("alice")))
                .thenReturn(emptyResponse);
        when(billingClient.getAiCreditUsage("org", currentMonth.getYear(),
                currentMonth.getMonthValue(), 1, "alice"))
                .thenReturn(usageResponse);
        when(billingClient.getBudgets("org", 100, 1, "user", "alice"))
                .thenReturn(buildBudgetsResponseWithEffectiveBudget(
                        effectiveBudget("user-alice", 10, 0.7),
                        userBudget("alice", 10),
                        userBudget("bob", 20),
                        matchingBudget(30, true)));

        var result = repository.findByOrgAndUserAndMonth("org", "alice", currentMonth);

        assertThat(result.getUserBudgetCapacityCredits()).isEqualByComparingTo("1000");
        assertThat(result.getUserBudgetConsumedCredits()).isEqualByComparingTo("70");
        assertThat(result.getRemainingUserBudgetCredits()).isEqualByComparingTo("930");
        assertThat(result.getUserBudgetUsageRatePercent()).isEqualByComparingTo("7");
        assertThat(result.isUniversalBudget()).isFalse();
        assertThat(result.isPreventFurtherUserBudgetUsage()).isTrue();
        verify(billingClient).getBudgets("org", 100, 1, "user", "alice");
        verify(billingClient, never())
                .getBudgets("org", 100, 1, "multi_user_customer", "alice");
    }

    @Test
    void findByOrgAndUserAndMonth_preserves_usage_when_budget_lookup_fails() {
        YearMonth currentMonth = YearMonth.now(java.time.ZoneOffset.UTC);
        WebApplicationException budgetFailure = webAppException(403);
        when(billingClient.getAiCreditUsage(eq("org"), eq(currentMonth.getYear()),
                eq(currentMonth.getMonthValue()), anyInt(), eq("alice")))
                .thenReturn(new AiCreditUsageResponse());
        when(billingClient.getAiCreditUsage("org", currentMonth.getYear(),
                currentMonth.getMonthValue(), 1, "alice"))
                .thenReturn(buildResponse(12, 1));
        when(billingClient.getBudgets("org", 100, 1, "user", "alice"))
                .thenThrow(budgetFailure);

        var result = repository.findByOrgAndUserAndMonth("org", "alice", currentMonth);

        assertThat(result.getDailyUsages()).hasSize(1);
        assertThat(result.getTotalGrossQuantity()).isEqualTo(12);
        assertThat(result.isUserBudgetLookupUnavailable()).isTrue();
        assertThat(result.isUserBudgetSet()).isFalse();
        verify(billingClient, never())
                .getBudgets("org", 100, 1, "multi_user_customer", "alice");
    }

    @ParameterizedTest
    @ValueSource(strings = {"user", "multi_user_customer"})
    void findByOrgAndUserAndMonth_preserves_usage_when_budget_processing_fails(String failedScope) {
        YearMonth currentMonth = YearMonth.now(java.time.ZoneOffset.UTC);
        when(billingClient.getAiCreditUsage(eq("org"), eq(currentMonth.getYear()),
                eq(currentMonth.getMonthValue()), anyInt(), eq("alice")))
                .thenReturn(new AiCreditUsageResponse());
        when(billingClient.getAiCreditUsage("org", currentMonth.getYear(),
                currentMonth.getMonthValue(), 1, "alice"))
                .thenReturn(buildResponse(12, 1));
        if ("multi_user_customer".equals(failedScope)) {
            when(billingClient.getBudgets("org", 100, 1, "user", "alice"))
                    .thenReturn(buildBudgetsResponse());
        }
        when(billingClient.getBudgets("org", 100, 1, failedScope, "alice"))
                .thenThrow(new ProcessingException("Budget request failed"));

        var result = repository.findByOrgAndUserAndMonth("org", "alice", currentMonth);

        assertThat(result.getDailyUsages()).hasSize(1);
        assertThat(result.getTotalGrossQuantity()).isEqualTo(12);
        assertThat(result.isUserBudgetLookupUnavailable()).isTrue();
        assertThat(result.isUserBudgetSet()).isFalse();
    }

    @Test
    void findByOrgAndUserAndMonth_uses_universal_budget_when_user_budget_is_unset() {
        YearMonth currentMonth = YearMonth.now(java.time.ZoneOffset.UTC);
        when(billingClient.getAiCreditUsage(eq("org"), eq(currentMonth.getYear()),
                eq(currentMonth.getMonthValue()), anyInt(), eq("alice")))
                .thenReturn(new AiCreditUsageResponse());
        when(billingClient.getAiCreditUsage("org", currentMonth.getYear(),
                currentMonth.getMonthValue(), 1, "alice"))
                .thenReturn(buildResponse(12, 1));
        when(billingClient.getBudgets("org", 100, 1, "user", "alice"))
                .thenReturn(buildBudgetsResponse());
        when(billingClient.getBudgets("org", 100, 1, "multi_user_customer", "alice"))
                .thenReturn(buildBudgetsResponseWithEffectiveBudget(
                        effectiveBudget("universal-1", 25, 4.5),
                        universalBudget("universal-1", 25, 9)));

        var result = repository.findByOrgAndUserAndMonth("org", "alice", currentMonth);

        assertThat(result.isUserBudgetSet()).isTrue();
        assertThat(result.isUniversalBudget()).isTrue();
        assertThat(result.getUserBudgetCapacityCredits()).isEqualByComparingTo("2500");
        assertThat(result.getUserBudgetConsumedCredits()).isEqualByComparingTo("450");
        assertThat(result.getRemainingUserBudgetCredits()).isEqualByComparingTo("2050");
        assertThat(result.getUserBudgetUsageRatePercent()).isEqualByComparingTo("18");
        verify(billingClient).getBudgets("org", 100, 1, "multi_user_customer", "alice");
    }

    @Test
    void findByOrgAndUserAndMonth_leaves_budget_unset_when_no_user_or_universal_budget_exists() {
        YearMonth currentMonth = YearMonth.now(java.time.ZoneOffset.UTC);
        when(billingClient.getAiCreditUsage(eq("org"), eq(currentMonth.getYear()),
                eq(currentMonth.getMonthValue()), anyInt(), eq("alice")))
                .thenReturn(new AiCreditUsageResponse());
        when(billingClient.getBudgets("org", 100, 1, "user", "alice"))
                .thenReturn(buildBudgetsResponse());
        when(billingClient.getBudgets("org", 100, 1, "multi_user_customer", "alice"))
                .thenReturn(buildBudgetsResponse());

        var result = repository.findByOrgAndUserAndMonth("org", "alice", currentMonth);

        assertThat(result.isUserBudgetSet()).isFalse();
        assertThat(result.getRemainingUserBudgetCredits()).isNull();
        assertThat(result.getUserBudgetUsageRatePercent()).isNull();
    }

    @Test
    void findByOrgAndUserAndMonth_does_not_fetch_user_budget_for_past_month() {
        YearMonth pastMonth = YearMonth.now(java.time.ZoneOffset.UTC).minusMonths(1);
        when(billingClient.getAiCreditUsage(eq("org"), eq(pastMonth.getYear()),
                eq(pastMonth.getMonthValue()), anyInt(), eq("alice")))
                .thenReturn(new AiCreditUsageResponse());

        var result = repository.findByOrgAndUserAndMonth("org", "alice", pastMonth);

        assertThat(result.isUserBudgetSet()).isFalse();
        verify(billingClient, never()).getBudgets(anyString(), anyInt(), anyInt(), anyString(), any());
    }

    // =========================================================================
    // findCopilotBillingInfo
    // =========================================================================

    @Test
    void findCopilotBillingInfo_returns_empty_on_404() {
        WebApplicationException ex = webAppException(404);
        when(billingClient.getCopilotBilling("org"))
                .thenThrow(ex);

        var result = repository.findCopilotBillingInfo("org");

        assertThat(result).isEmpty();
    }

    @Test
    void findCopilotBillingInfo_returns_empty_on_null_response() {
        when(billingClient.getCopilotBilling("org")).thenReturn(null);

        assertThat(repository.findCopilotBillingInfo("org")).isEmpty();
    }

    @Test
    void findCopilotBillingInfo_maps_response_to_domain() {
        SeatBreakdownDto seatBreakdown = new SeatBreakdownDto();
        seatBreakdown.setTotal(42);
        CopilotBillingResponse response = new CopilotBillingResponse();
        response.setSeatBreakdown(seatBreakdown);
        response.setPlanType("business");
        when(billingClient.getCopilotBilling("org")).thenReturn(response);

        var result = repository.findCopilotBillingInfo("org");

        assertThat(result).isPresent();
        assertThat(result.get().getTotalSeats()).isEqualTo(42);
        assertThat(result.get().getPlanType()).isEqualTo("business");
    }

    @Test
    void findCopilotBillingInfo_throws_on_non_404_client_error() {
        WebApplicationException ex = webAppException(403);
        when(billingClient.getCopilotBilling("org"))
                .thenThrow(ex);

        assertThatThrownBy(() -> repository.findCopilotBillingInfo("org"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus())
                .isEqualTo(403);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    @Test
    void findCopilotSeatAssignmentDates_readsEveryPageAndNormalizesToUtc() {
        CopilotSeatsResponse firstPage = new CopilotSeatsResponse();
        firstPage.setSeats(Collections.nCopies(100, seat("2026-10-01T00:00:00Z")));
        CopilotSeatsResponse secondPage = new CopilotSeatsResponse();
        secondPage.setSeats(List.of(seat("2026-11-02T00:30:00+09:00")));
        when(billingClient.getCopilotSeats("org", 100, 1)).thenReturn(firstPage);
        when(billingClient.getCopilotSeats("org", 100, 2)).thenReturn(secondPage);

        var dates = repository.findCopilotSeatAssignmentDates("org");

        assertThat(dates).hasSize(101);
        assertThat(dates.get(100)).isEqualTo(LocalDate.of(2026, 11, 1));
        verify(billingClient, never()).getCopilotSeats("org", 100, 3);
    }

    @Test
    void findCopilotSeatAssignmentDate_mapsQueriedUserSeat() {
        when(billingClient.getCopilotSeat("org", "alice"))
                .thenReturn(seat("2026-11-02T09:33:14+09:00"));

        assertThat(repository.findCopilotSeatAssignmentDate("org", "alice"))
                .contains(LocalDate.of(2026, 11, 2));
    }

    @Test
    void findCopilotSeatAssignmentDate_returnsEmptyForMissingSeat() {
        WebApplicationException ex = webAppException(404);
        when(billingClient.getCopilotSeat("org", "alice")).thenThrow(ex);

        assertThat(repository.findCopilotSeatAssignmentDate("org", "alice")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {403, 429, 503})
    void seatLookups_surfaceFailuresRatherThanFullSeatFallback(int status) {
        WebApplicationException ex = webAppException(status);
        when(billingClient.getCopilotSeats("org", 100, 1)).thenThrow(ex);
        when(billingClient.getCopilotSeat("org", "alice")).thenThrow(ex);

        assertThatThrownBy(() -> repository.findCopilotSeatAssignmentDates("org"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus()).isEqualTo(status);
        assertThatThrownBy(() -> repository.findCopilotSeatAssignmentDate("org", "alice"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus()).isEqualTo(status);
    }

    @Test
    void findCopilotSeatAssignmentDates_returnsEmptyForNoSeats() {
        when(billingClient.getCopilotSeats("org", 100, 1)).thenReturn(new CopilotSeatsResponse());

        assertThat(repository.findCopilotSeatAssignmentDates("org")).isEmpty();
    }

    @Test
    void findCopilotSeatAssignmentDate_rejectsMissingAssignmentDate() {
        when(billingClient.getCopilotSeat("org", "alice")).thenReturn(new CopilotSeatDto());

        assertThatThrownBy(() -> repository.findCopilotSeatAssignmentDate("org", "alice"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus()).isEqualTo(502);
    }

    @Test
    void findCopilotSeatAssignmentDate_rejectsInvalidAssignmentDate() {
        when(billingClient.getCopilotSeat("org", "alice")).thenReturn(seat("not-a-date"));

        assertThatThrownBy(() -> repository.findCopilotSeatAssignmentDate("org", "alice"))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus()).isEqualTo(502);
    }

    private static CopilotSeatDto seat(String createdAt) {
        CopilotSeatDto seat = new CopilotSeatDto();
        seat.setCreatedAt(createdAt);
        return seat;
    }

    private static AiCreditUsageResponse buildResponse(double qty, double amount) {
        UsageItemDto dto = new UsageItemDto();
        dto.setProduct("Copilot");
        dto.setSku("AI Credits");
        dto.setModel("gpt-4o");
        dto.setUnitType("credits");
        dto.setGrossQuantity(qty);
        dto.setDiscountQuantity(qty);
        dto.setNetQuantity(qty);
        dto.setNetAmount(amount);

        AiCreditUsageResponse resp = new AiCreditUsageResponse();
        resp.setUsageItems(List.of(dto));
        return resp;
    }

    private static BudgetsResponse buildBudgetsResponse(BudgetDto... budgets) {
        return buildBudgetsResponse(false, budgets);
    }

    private static BudgetsResponse buildBudgetsResponse(boolean hasNextPage, BudgetDto... budgets) {
        BudgetsResponse response = new BudgetsResponse();
        response.setBudgets(List.of(budgets));
        response.setHasNextPage(hasNextPage);
        return response;
    }

    private static BudgetsResponse buildBudgetsResponseWithEffectiveBudget(
            BudgetDto effectiveBudget, BudgetDto... budgets) {
        BudgetsResponse response = buildBudgetsResponse(budgets);
        response.setEffectiveBudget(effectiveBudget);
        return response;
    }

    private static BudgetDto matchingBudget(double budgetUsd, boolean preventFurtherUsage) {
        BudgetDto budget = new BudgetDto();
        budget.setBudgetScope("organization");
        budget.setBudgetType("BundlePricing");
        budget.setBudgetProductSku("ai_credits");
        budget.setBudgetAmount(budgetUsd);
        budget.setPreventFurtherUsage(preventFurtherUsage);
        return budget;
    }

    private static BudgetDto universalBudget(String id, double budgetUsd, double consumedUsd) {
        BudgetDto budget = new BudgetDto();
        budget.setId(id);
        budget.setBudgetScope("multi_user_customer");
        budget.setBudgetType("BundlePricing");
        budget.setBudgetProductSku("ai_credits");
        budget.setBudgetAmount(budgetUsd);
        budget.setConsumedAmount(consumedUsd);
        budget.setPreventFurtherUsage(true);
        return budget;
    }

    private static BudgetDto effectiveBudget(String id, double budgetUsd, double consumedUsd) {
        BudgetDto budget = new BudgetDto();
        budget.setId(id);
        budget.setBudgetAmount(budgetUsd);
        budget.setConsumedAmount(consumedUsd);
        return budget;
    }

    private static BudgetDto userBudget(String login, double budgetUsd) {
        BudgetDto budget = new BudgetDto();
        budget.setId("user-" + login);
        budget.setBudgetScope("user");
        budget.setUser(login);
        budget.setBudgetType("BundlePricing");
        budget.setBudgetProductSku("ai_credits");
        budget.setBudgetAmount(budgetUsd);
        budget.setPreventFurtherUsage(true);
        return budget;
    }

    private static BudgetDto repositoryBudget() {
        BudgetDto budget = new BudgetDto();
        budget.setBudgetScope("repository");
        budget.setBudgetType("BundlePricing");
        budget.setBudgetProductSku("ai_credits");
        budget.setBudgetAmount(99.0);
        return budget;
    }

    /**
     * Build a WebApplicationException backed by a mocked Response.
     * Uses the real WebApplicationException(Response) constructor to avoid
     * nested mock()/when() calls that would leave an UnfinishedStubbing if
     * this helper were evaluated inside a thenThrow() argument.
     * Always call this method BEFORE starting any outer when() chain.
     */
    private WebApplicationException webAppException(int status) {
        Response.StatusType statusInfo = mock(Response.StatusType.class);
        lenient().when(statusInfo.getStatusCode()).thenReturn(status);
        lenient().when(statusInfo.getFamily()).thenReturn(Response.Status.Family.familyOf(status));
        lenient().when(statusInfo.getReasonPhrase()).thenReturn("Status " + status);

        Response resp = mock(Response.class);
        lenient().when(resp.getStatus()).thenReturn(status);
        lenient().when(resp.getStatusInfo()).thenReturn(statusInfo);
        return new WebApplicationException(resp);
    }
}
