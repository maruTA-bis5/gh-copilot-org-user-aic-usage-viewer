package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level response from {@code GET /orgs/{org}/copilot/billing}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopilotBillingResponse {

    @JsonProperty("seat_breakdown")
    private SeatBreakdownDto seatBreakdown;

    @JsonProperty("plan_type")
    private String planType;

    public SeatBreakdownDto getSeatBreakdown() { return seatBreakdown; }
    public void setSeatBreakdown(SeatBreakdownDto seatBreakdown) { this.seatBreakdown = seatBreakdown; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }
}
