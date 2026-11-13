package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Budget summary from {@code GET /organizations/{org}/settings/billing/budgets}.
 *
 * <p>{@code budget_amount} is returned by GitHub in USD and is converted to
 * AI credits at the repository boundary before domain calculations.</p>
 */
public class BudgetDto {

    private String id;

    @JsonbProperty("budget_type")
    private String budgetType;

    @JsonbProperty("budget_product_sku")
    private String budgetProductSku;

    @JsonbProperty("budget_scope")
    private String budgetScope;

    private String user;

    @JsonbProperty("budget_amount")
    private Double budgetAmount;

    @JsonbProperty("consumed_amount")
    private Double consumedAmount;

    @JsonbProperty("prevent_further_usage")
    private boolean preventFurtherUsage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBudgetType() { return budgetType; }
    public void setBudgetType(String budgetType) { this.budgetType = budgetType; }

    public String getBudgetProductSku() { return budgetProductSku; }
    public void setBudgetProductSku(String budgetProductSku) { this.budgetProductSku = budgetProductSku; }

    public String getBudgetScope() { return budgetScope; }
    public void setBudgetScope(String budgetScope) { this.budgetScope = budgetScope; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public Double getBudgetAmount() { return budgetAmount; }
    public void setBudgetAmount(Double budgetAmount) { this.budgetAmount = budgetAmount; }

    public Double getConsumedAmount() { return consumedAmount; }
    public void setConsumedAmount(Double consumedAmount) { this.consumedAmount = consumedAmount; }

    public boolean isPreventFurtherUsage() { return preventFurtherUsage; }
    public void setPreventFurtherUsage(boolean preventFurtherUsage) { this.preventFurtherUsage = preventFurtherUsage; }
}
