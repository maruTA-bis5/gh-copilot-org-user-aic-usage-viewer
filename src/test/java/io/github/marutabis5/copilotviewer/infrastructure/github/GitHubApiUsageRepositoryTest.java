package io.github.marutabis5.copilotviewer.infrastructure.github;

import io.github.marutabis5.copilotviewer.infrastructure.github.dto.AiCreditUsageResponse;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.UsageItemDto;
import io.github.marutabis5.copilotviewer.service.GitHubApiException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GitHubApiUsageRepository}, focusing on retry logic
 * and response mapping.
 */
@ExtendWith(MockitoExtension.class)
class GitHubApiUsageRepositoryTest {

    @Mock
    io.github.marutabis5.copilotviewer.infrastructure.github.GitHubBillingClient billingClient;

    @InjectMocks
    GitHubApiUsageRepository repository;

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
    // fetchDayWithRetry – retry logic
    // =========================================================================

    @Test
    void fetchDay_retries_on_429_then_succeeds() {
        // Pre-create exceptions BEFORE starting any when() chain to avoid
        // UnfinishedStubbing caused by nested mock/when() calls.
        WebApplicationException ex = webAppException(429);
        AiCreditUsageResponse resp = buildResponse(10, 1.0);

        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex)
                .thenReturn(resp);

        GitHubApiUsageRepository fast = fastRepo();

        AiCreditUsageResponse result = fast.fetchDayWithRetry("org", "user", 2025, 1, 1);

        assertThat(result).isSameAs(resp);
        verify(billingClient, times(2))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void fetchDay_retries_on_503_then_succeeds() {
        WebApplicationException ex = webAppException(503);
        AiCreditUsageResponse resp = buildResponse(5, 0.5);

        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex)
                .thenReturn(resp);

        GitHubApiUsageRepository fast = fastRepo();

        AiCreditUsageResponse result = fast.fetchDayWithRetry("org", "user", 2025, 1, 1);

        assertThat(result).isSameAs(resp);
        verify(billingClient, times(2))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void fetchDay_exhausts_retries_and_throws_GitHubApiException() {
        WebApplicationException ex1 = webAppException(500);
        WebApplicationException ex2 = webAppException(500);
        WebApplicationException ex3 = webAppException(500);

        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex1)
                .thenThrow(ex2)
                .thenThrow(ex3);

        GitHubApiUsageRepository fast = fastRepo();

        assertThatThrownBy(() -> fast.fetchDayWithRetry("org", "user", 2025, 1, 1))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus())
                .isEqualTo(500);

        // 1 initial + 2 retries = 3 total calls
        verify(billingClient, times(GitHubApiUsageRepository.MAX_RETRIES + 1))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void fetchDay_does_not_retry_on_403() {
        WebApplicationException ex = webAppException(403);

        when(billingClient.getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenThrow(ex);

        assertThatThrownBy(() -> repository.fetchDayWithRetry("org", "user", 2025, 1, 1))
                .isInstanceOf(GitHubApiException.class)
                .extracting(e -> ((GitHubApiException) e).getHttpStatus())
                .isEqualTo(403);

        // Must not retry on non-retryable status
        verify(billingClient, times(1))
                .getAiCreditUsage(anyString(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void fetchDay_does_not_retry_on_404() {
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
    // findByOrgAndUserAndMonth – month boundary
    // =========================================================================

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

    // =========================================================================
    // Helpers
    // =========================================================================

    private static AiCreditUsageResponse buildResponse(double qty, double amount) {
        UsageItemDto dto = new UsageItemDto();
        dto.setProduct("Copilot");
        dto.setSku("AI Credits");
        dto.setModel("gpt-4o");
        dto.setUnitType("credits");
        dto.setGrossQuantity(qty);
        dto.setNetQuantity(qty);
        dto.setNetAmount(amount);

        AiCreditUsageResponse resp = new AiCreditUsageResponse();
        resp.setUsageItems(List.of(dto));
        return resp;
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

    /** Returns a GitHubApiUsageRepository that skips real sleep delays. */
    private GitHubApiUsageRepository fastRepo() {
        GitHubApiUsageRepository fast = new GitHubApiUsageRepository() {
            @Override
            void sleepForTest(long ms) { /* no-op */ }
        };
        injectClient(fast);
        return fast;
    }

    private void injectClient(GitHubApiUsageRepository repo) {
        try {
            Field f = GitHubApiUsageRepository.class.getDeclaredField("billingClient");
            f.setAccessible(true);
            f.set(repo, billingClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
