package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import java.util.List;

public class CopilotSeatsResponse {

    private List<CopilotSeatDto> seats = List.of();

    public List<CopilotSeatDto> getSeats() { return seats; }
    public void setSeats(List<CopilotSeatDto> seats) { this.seats = seats; }
}
