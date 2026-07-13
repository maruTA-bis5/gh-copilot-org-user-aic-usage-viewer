package io.github.marutabis5.copilotviewer.faces.chart;

import io.github.marutabis5.copilotviewer.domain.model.DailyUsage;
import io.github.marutabis5.copilotviewer.domain.model.UsageItem;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.category.CategoryScaleOptions;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds Chart.js line-chart JSON for daily gross-usage trends.
 */
public class UsageTrendChartJsonBuilder {

    private static final List<SeriesColor> COLOR_PALETTE = List.of(
            new SeriesColor("#0d6efd", "rgba(13, 110, 253, 0.30)"),
            new SeriesColor("#198754", "rgba(25, 135, 84, 0.30)"),
            new SeriesColor("#fd7e14", "rgba(253, 126, 20, 0.30)"),
            new SeriesColor("#6f42c1", "rgba(111, 66, 193, 0.30)"),
            new SeriesColor("#dc3545", "rgba(220, 53, 69, 0.30)"),
            new SeriesColor("#20c997", "rgba(32, 201, 151, 0.30)")
    );

    public String buildChartJson(List<String> labels,
                                 Map<String, List<Double>> seriesByName,
                                 boolean cumulative) {
        Objects.requireNonNull(labels, "labels must not be null");
        Objects.requireNonNull(seriesByName, "seriesByName must not be null");

        LineData data = LineChart.data().setLabels(labels);
        int colorIndex = 0;
        for (Map.Entry<String, List<Double>> series : seriesByName.entrySet()) {
            validateSeriesLength(labels, series);
            SeriesColor color = COLOR_PALETTE.get(colorIndex % COLOR_PALETTE.size());
            List<Number> values = new ArrayList<>(toChartValues(series.getValue(), cumulative));
            LineDataset dataset = new LineDataset()
                    .setLabel(series.getKey())
                    .setData(values)
                    .setBorderColor(color.borderColor())
                    .setBackgroundColor(color.backgroundColor())
                    .setFill(cumulative)
                    .setStack("gross");
            data.addDataset(dataset);
            colorIndex++;
        }

        LineOptions options = LineChart.options()
                .setResponsive(true)
                .setMaintainAspectRatio(false)
                .setScales(new Scales()
                        .addScale("x", new CategoryScaleOptions().setStacked(cumulative))
                        .addScale("y", new LinearScaleOptions()
                                .setBeginAtZero(true)
                                .setStacked(cumulative)));

        return new LineChart(data, options).toJson();
    }

    public String buildSingleTotalSeries(List<DailyUsage> dailyUsages,
                                         String seriesLabel,
                                         boolean cumulative) {
        Objects.requireNonNull(dailyUsages, "dailyUsages must not be null");
        String nonNullSeriesLabel = Objects.requireNonNull(seriesLabel, "seriesLabel must not be null");

        List<String> labels = new ArrayList<>(dailyUsages.size());
        List<Double> values = new ArrayList<>(dailyUsages.size());
        for (DailyUsage day : dailyUsages) {
            labels.add(day.getDate().toString());
            values.add(day.getTotalGrossQuantity());
        }
        return buildChartJson(labels, Map.of(nonNullSeriesLabel, values), cumulative);
    }

    public String buildPerModelSeries(List<DailyUsage> dailyUsages, boolean cumulative) {
        Objects.requireNonNull(dailyUsages, "dailyUsages must not be null");

        List<DailyUsage> sortedUsages = dailyUsages.stream()
                .sorted(Comparator.comparing(DailyUsage::getDate))
                .toList();
        List<String> labels = sortedUsages.stream()
                .map(day -> day.getDate().toString())
                .toList();

        Map<String, double[]> valuesByModel = new LinkedHashMap<>();
        for (int dayIndex = 0; dayIndex < sortedUsages.size(); dayIndex++) {
            final int index = dayIndex;
            Map<String, Double> dayTotalsByModel = sortedUsages.get(dayIndex).getItems().stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            item -> normalizeModel(item.getModel()),
                            java.util.stream.Collectors.summingDouble(UsageItem::getGrossQuantity)));
            dayTotalsByModel.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> valuesByModel
                            .computeIfAbsent(entry.getKey(), key -> new double[sortedUsages.size()])[index]
                            = entry.getValue());
        }

        Map<String, List<Double>> seriesByModel = new LinkedHashMap<>();
        valuesByModel.forEach((model, values) -> {
            List<Double> seriesValues = new ArrayList<>(values.length);
            for (double value : values) {
                seriesValues.add(value);
            }
            seriesByModel.put(model, seriesValues);
        });

        return buildChartJson(labels, seriesByModel, cumulative);
    }

    private static List<Double> toChartValues(List<Double> perDayValues, boolean cumulative) {
        if (!cumulative) {
            return perDayValues;
        }
        List<Double> runningTotals = new ArrayList<>(perDayValues.size());
        double sum = 0d;
        for (Double value : perDayValues) {
            sum += value;
            runningTotals.add(sum);
        }
        return runningTotals;
    }

    private static void validateSeriesLength(List<String> labels, Map.Entry<String, List<Double>> series) {
        if (series.getValue().size() != labels.size()) {
            throw new IllegalArgumentException(
                    "Series '%s' size (%d) does not match labels size (%d).".formatted(
                            series.getKey(), series.getValue().size(), labels.size()));
        }
    }

    private static String normalizeModel(String model) {
        if (model == null || model.isBlank()) {
            return "(unknown)";
        }
        return model;
    }

    private record SeriesColor(String borderColor, String backgroundColor) {
    }
}
