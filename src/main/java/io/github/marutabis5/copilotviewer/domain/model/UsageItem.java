package io.github.marutabis5.copilotviewer.domain.model;

import java.util.Objects;

/**
 * A single line item within a day's AI credit usage report.
 * Represents usage by a specific product / SKU / model combination.
 */
public final class UsageItem {

    private final String product;
    private final String sku;
    private final String model;
    private final String unitType;
    private final double grossQuantity;
    private final double discountQuantity;
    private final double netQuantity;
    private final double netAmount;

    public UsageItem(String product,
                     String sku,
                     String model,
                     String unitType,
                     double grossQuantity,
                     double discountQuantity,
                     double netQuantity,
                     double netAmount) {
        this.product = product;
        this.sku = sku;
        this.model = model;
        this.unitType = unitType;
        this.grossQuantity = grossQuantity;
        this.discountQuantity = discountQuantity;
        this.netQuantity = netQuantity;
        this.netAmount = netAmount;
    }

    public String getProduct() { return product; }
    public String getSku() { return sku; }
    public String getModel() { return model; }
    public String getUnitType() { return unitType; }
    public double getGrossQuantity() { return grossQuantity; }
    public double getDiscountQuantity() { return discountQuantity; }
    public double getNetQuantity() { return netQuantity; }
    public double getNetAmount() { return netAmount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UsageItem that)) return false;
        return Double.compare(netQuantity, that.netQuantity) == 0
                && Double.compare(netAmount, that.netAmount) == 0
                && Double.compare(grossQuantity, that.grossQuantity) == 0
                && Double.compare(discountQuantity, that.discountQuantity) == 0
                && Objects.equals(product, that.product)
                && Objects.equals(sku, that.sku)
                && Objects.equals(model, that.model)
                && Objects.equals(unitType, that.unitType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, sku, model, unitType, grossQuantity, discountQuantity, netQuantity, netAmount);
    }

    @Override
    public String toString() {
        return "UsageItem{product='%s', sku='%s', model='%s', netQuantity=%.4f, netAmount=%.4f}"
                .formatted(product, sku, model, netQuantity, netAmount);
    }
}
