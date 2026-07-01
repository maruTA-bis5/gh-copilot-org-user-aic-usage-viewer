package io.github.marutabis5.copilotviewer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.stream.Stream;

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

    @ParameterizedTest(name = "{0}")
    @MethodSource("yearMonthValidationCases")
    void yearMonth_validation_across_input_types(String scenario,
                                                 InputType inputType,
                                                 Object input,
                                                 YearMonth expected,
                                                 String expectedErrorPart) {
        if (expected != null) {
            assertThat(validateYearMonth(inputType, input)).isEqualTo(expected);
            return;
        }
        assertThatThrownBy(() -> validateYearMonth(inputType, input))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining(expectedErrorPart);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025", "25-06", "2025/06", "2025-13", "abcd-ef", ""})
    void yearMonth_invalid_format_throws(String bad) {
        assertThatThrownBy(() -> CopilotUsageService.validateYearMonth(bad))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void yearMonth_leading_trailing_spaces_are_trimmed() {
        YearMonth result = CopilotUsageService.validateYearMonth("  2024-06  ");
        assertThat(result).isEqualTo(YearMonth.of(2024, 6));
    }

    private static Stream<Arguments> yearMonthValidationCases() {
        YearMonth current = YearMonth.now(ZoneOffset.UTC);
        YearMonth past = YearMonth.of(2024, 1);
        YearMonth future = current.plusMonths(1);

        return Stream.of(
                Arguments.of("current month String", InputType.STRING, current.toString(), current, null),
                Arguments.of("current month YearMonth", InputType.YEAR_MONTH, current, current, null),
                Arguments.of("past month String", InputType.STRING, "2024-01", past, null),
                Arguments.of("past month YearMonth", InputType.YEAR_MONTH, past, past, null),
                Arguments.of("future month String", InputType.STRING, future.toString(), null, "Future months are not allowed"),
                Arguments.of("future month YearMonth", InputType.YEAR_MONTH, future, null, "Future months are not allowed"),
                Arguments.of("null String", InputType.STRING, null, null, "empty"),
                Arguments.of("null YearMonth", InputType.YEAR_MONTH, null, null, "empty")
        );
    }

    private static YearMonth validateYearMonth(InputType inputType, Object input) {
        return switch (inputType) {
            case STRING -> CopilotUsageService.validateYearMonth((String) input);
            case YEAR_MONTH -> CopilotUsageService.validateYearMonth((YearMonth) input);
        };
    }

    private enum InputType {
        STRING,
        YEAR_MONTH
    }
}
