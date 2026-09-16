package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import jakarta.json.bind.annotation.JsonbProperty;

import java.util.Collections;
import java.util.List;

/**
 * Top-level response from {@code GET /organizations/{org}/settings/billing/budgets}.
 */
public class BudgetsResponse {

    private List<BudgetDto> budgets;

    @JsonbProperty("has_next_page")
    private boolean hasNextPage;

    public List<BudgetDto> getBudgets() {
        return budgets != null ? budgets : Collections.emptyList();
    }

    public void setBudgets(List<BudgetDto> budgets) { this.budgets = budgets; }

    public boolean isHasNextPage() { return hasNextPage; }
    public void setHasNextPage(boolean hasNextPage) { this.hasNextPage = hasNextPage; }
}
