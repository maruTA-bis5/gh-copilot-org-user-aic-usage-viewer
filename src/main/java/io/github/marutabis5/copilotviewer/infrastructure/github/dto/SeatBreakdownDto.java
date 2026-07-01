package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

/**
 * Seat breakdown object within the Copilot billing response.
 */
public class SeatBreakdownDto {

    private int total;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }
}
