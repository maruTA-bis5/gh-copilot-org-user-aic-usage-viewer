package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

/**
 * Top-level response from {@code GET /organizations/{org}/settings/billing/ai_credit/usage}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiCreditUsageResponse {

    private TimePeriodDto timePeriod;
    private String organization;
    private List<UsageItemDto> usageItems;

    public TimePeriodDto getTimePeriod() { return timePeriod; }
    public void setTimePeriod(TimePeriodDto timePeriod) { this.timePeriod = timePeriod; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public List<UsageItemDto> getUsageItems() {
        return usageItems != null ? usageItems : Collections.emptyList();
    }
    public void setUsageItems(List<UsageItemDto> usageItems) { this.usageItems = usageItems; }
}
