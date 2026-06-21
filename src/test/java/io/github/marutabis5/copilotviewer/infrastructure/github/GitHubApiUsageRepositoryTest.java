package io.github.marutabis5.copilotviewer.infrastructure.github;

import io.github.marutabis5.copilotviewer.infrastructure.github.dto.AiCreditUsageResponse;
import io.github.marutabis5.copilotviewer.infrastructure.github.dto.UsageItemDto;
import io.github.marutabis5.copilotviewer.service.GitHubApiException;
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
import java.util.List;

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
}
