package io.github.marutabis5.copilotviewer.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Copilot billing subscription information for an organisation.
 */
public final class CopilotBillingInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int totalSeats;
    private final String planType;

    public CopilotBillingInfo(int totalSeats, String planType) {
        this.totalSeats = totalSeats;
        this.planType = Objects.requireNonNull(planType, "planType must not be null");
    }

    public int getTotalSeats() { return totalSeats; }
    public String getPlanType() { return planType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CopilotBillingInfo that)) return false;
        return totalSeats == that.totalSeats && Objects.equals(planType, that.planType);
    }

    @Override
    public int hashCode() { return Objects.hash(totalSeats, planType); }

    @Override
    public String toString() {
        return "CopilotBillingInfo{totalSeats=%d, planType='%s'}".formatted(totalSeats, planType);
    }
}
