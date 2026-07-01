package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Top-level response from {@code GET /orgs/{org}/copilot/billing}.
 */
public class CopilotBillingResponse {

    @JsonbProperty("seat_breakdown")
    private SeatBreakdownDto seatBreakdown;

    @JsonbProperty("plan_type")
    private String planType;

    public SeatBreakdownDto getSeatBreakdown() { return seatBreakdown; }
    public void setSeatBreakdown(SeatBreakdownDto seatBreakdown) { this.seatBreakdown = seatBreakdown; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
}
