package io.github.marutabis5.copilotviewer.faces.chart;

import io.github.marutabis5.copilotviewer.domain.model.DailyUsage;
import io.github.marutabis5.copilotviewer.domain.model.UsageItem;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UsageTrendChartJsonBuilderTest {

    private final UsageTrendChartJsonBuilder builder = new UsageTrendChartJsonBuilder();

    @Test
    void buildSingleTotalSeries_includes_label_dataset_and_cumulative_values() {
        DailyUsage first = new DailyUsage(LocalDate.of(2025, 1, 1), List.of(
                new UsageItem("Copilot", "AI Credits", "gpt-4o", "credits", 10, 0, 10, 1.0)));
        DailyUsage second = new DailyUsage(LocalDate.of(2025, 1, 2), List.of(
                new UsageItem("Copilot", "AI Credits", "gpt-4o", "credits", 5, 0, 5, 0.5)));

        String json = builder.buildSingleTotalSeries(List.of(first, second), "octocat", true);

        assertThat(json).contains("\"type\":\"line\"");
        assertThat(json).contains("\"labels\":[\"2025-01-01\",\"2025-01-02\"]");
        assertThat(json).contains("\"label\":\"octocat\"");
        assertThat(json).contains("\"data\":[10.0,15.0]");
        assertThat(json).contains("\"stacked\":true");
    }

    @Test
    void buildPerModelSeries_aggregates_by_model_per_day() {
        DailyUsage first = new DailyUsage(LocalDate.of(2025, 1, 1), List.of(
                new UsageItem("Copilot", "AI Credits", "gpt-4o", "credits", 10, 0, 10, 1.0),
                new UsageItem("Copilot", "AI Credits", "gpt-4.1", "credits", 3, 0, 3, 0.3)));
        DailyUsage second = new DailyUsage(LocalDate.of(2025, 1, 2), List.of(
                new UsageItem("Copilot", "AI Credits", "gpt-4o", "credits", 5, 0, 5, 0.5)));

        String json = builder.buildPerModelSeries(List.of(first, second), false);

        assertThat(json).contains("\"labels\":[\"2025-01-01\",\"2025-01-02\"]");
        assertThat(json).contains("\"label\":\"gpt-4.1\"");
        assertThat(json).contains("\"label\":\"gpt-4o\"");
        assertThat(json).contains("\"data\":[3.0,0.0]");
        assertThat(json).contains("\"data\":[10.0,5.0]");
        assertThat(json).contains("\"stacked\":false");
    }

    @Test
    void buildPerModelSeries_aggregates_and_accumulates_when_enabled() {
        DailyUsage first = new DailyUsage(LocalDate.of(2025, 1, 1), List.of(
                new UsageItem("Copilot", "AI Credits", "gpt-4o", "credits", 10, 0, 10, 1.0),
                new UsageItem("Copilot", "AI Credits", "gpt-4.1", "credits", 3, 0, 3, 0.3)));
        DailyUsage second = new DailyUsage(LocalDate.of(2025, 1, 2), List.of(
                new UsageItem("Copilot", "AI Credits", "gpt-4o", "credits", 5, 0, 5, 0.5)));

        String json = builder.buildPerModelSeries(List.of(first, second), true);

        assertThat(json).contains("\"data\":[3.0,3.0]");
        assertThat(json).contains("\"data\":[10.0,15.0]");
        assertThat(json).contains("\"stacked\":true");
    }
}
