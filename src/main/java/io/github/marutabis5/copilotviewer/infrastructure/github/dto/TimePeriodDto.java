package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The {@code timePeriod} object returned by the GitHub billing API.
 * Fields are nullable because the API may return only the queried granularity
 * (e.g. only {@code year} when no month/day was specified).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimePeriodDto {

    private Integer year;
    private Integer month;
    private Integer day;

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getDay() { return day; }
    public void setDay(Integer day) { this.day = day; }
}
