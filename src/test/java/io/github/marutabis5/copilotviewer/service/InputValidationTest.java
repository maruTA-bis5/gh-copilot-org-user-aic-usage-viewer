package io.github.marutabis5.copilotviewer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the input validation logic in {@link CopilotUsageService}.
 * Validation methods are package-private statics, so no container is needed.
 */
class InputValidationTest {

    // =========================================================================
    // validateLogin
    // =========================================================================

    @Test
    void login_valid_trimmed() {
        assertThat(CopilotUsageService.validateLogin("  octocat  ")).isEqualTo("octocat");
    }

    @Test
    void login_exactly_max_length_is_accepted() {
        String thirtyNine = "a".repeat(39);
        assertThat(CopilotUsageService.validateLogin(thirtyNine)).isEqualTo(thirtyNine);
    }

    @Test
    void login_null_throws() {
        assertThatThrownBy(() -> CopilotUsageService.validateLogin(null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void login_blank_throws() {
        assertThatThrownBy(() -> CopilotUsageService.validateLogin("   "))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void login_too_long_throws() {
        String forty = "a".repeat(40);
        assertThatThrownBy(() -> CopilotUsageService.validateLogin(forty))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("39");
    }

    // =========================================================================
    // validateYearMonth
    // =========================================================================

    @Test
    void yearMonth_current_month_is_accepted_String() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        YearMonth result = CopilotUsageService.validateYearMonth(currentMonth.toString()); // "YYYY-MM"
        assertThat(result).isEqualTo(currentMonth);
    }

    @Test
    void yearMonth_current_month_is_accepted_YearMonth() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        YearMonth result = CopilotUsageService.validateYearMonth(currentMonth);
        assertThat(result).isEqualTo(currentMonth);
    }

    @Test
    void yearMonth_past_month_is_accepted_String() {
        YearMonth result = CopilotUsageService.validateYearMonth("2024-01");
        assertThat(result).isEqualTo(YearMonth.of(2024, 1));
    }

    @Test
    void yearMonth_past_month_is_accepted_YearMonth() {
        YearMonth result = CopilotUsageService.validateYearMonth(YearMonth.of(2024, 1));
        assertThat(result).isEqualTo(YearMonth.of(2024, 1));
    }

    @Test
    void yearMonth_future_month_throws_String() {
        YearMonth future = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        assertThatThrownBy(() -> CopilotUsageService.validateYearMonth(future.toString()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Future months are not allowed");
    }

    @Test
    void yearMonth_future_month_throws_YearMonth() {
        YearMonth future = YearMonth.now(ZoneOffset.UTC).plusMonths(1);
        assertThatThrownBy(() -> CopilotUsageService.validateYearMonth(future))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Future months are not allowed");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025", "25-06", "2025/06", "2025-13", "abcd-ef", ""})
    void yearMonth_invalid_format_throws(String bad) {
        assertThatThrownBy(() -> CopilotUsageService.validateYearMonth(bad))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void yearMonth_null_throws_String() {
        assertThatThrownBy(() -> CopilotUsageService.validateYearMonth((String)null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void yearMonth_null_throws_YearMonth() {
        assertThatThrownBy(() -> CopilotUsageService.validateYearMonth((YearMonth)null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void yearMonth_leading_trailing_spaces_are_trimmed() {
        YearMonth result = CopilotUsageService.validateYearMonth("  2024-06  ");
        assertThat(result).isEqualTo(YearMonth.of(2024, 6));
    }
}
