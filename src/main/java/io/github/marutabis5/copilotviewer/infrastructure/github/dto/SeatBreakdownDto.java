package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Seat breakdown object within the Copilot billing response.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeatBreakdownDto {

    private int total;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
