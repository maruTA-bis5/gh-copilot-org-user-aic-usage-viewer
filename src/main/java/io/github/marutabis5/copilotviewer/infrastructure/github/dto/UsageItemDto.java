package io.github.marutabis5.copilotviewer.infrastructure.github.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One line item from the GitHub Billing AI Credit usage API response.
 *
 * <p>Endpoint: {@code GET /organizations/{org}/settings/billing/ai_credit/usage}</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class UsageItemDto {

    private String product;
    private String sku;
    private String model;
    private String unitType;
    private double pricePerUnit;
    private double grossQuantity;
    private double grossAmount;
    private double discountQuantity;
    private double discountAmount;
    private double netQuantity;
    private double netAmount;

    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getUnitType() { return unitType; }
    public void setUnitType(String unitType) { this.unitType = unitType; }

    public double getPricePerUnit() { return pricePerUnit; }
    public void setPricePerUnit(double pricePerUnit) { this.pricePerUnit = pricePerUnit; }

    public double getGrossQuantity() { return grossQuantity; }
    public void setGrossQuantity(double grossQuantity) { this.grossQuantity = grossQuantity; }

    public double getGrossAmount() { return grossAmount; }
    public void setGrossAmount(double grossAmount) { this.grossAmount = grossAmount; }

    public double getDiscountQuantity() { return discountQuantity; }
    public void setDiscountQuantity(double discountQuantity) { this.discountQuantity = discountQuantity; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getNetQuantity() { return netQuantity; }
    public void setNetQuantity(double netQuantity) { this.netQuantity = netQuantity; }

    public double getNetAmount() { return netAmount; }
    public void setNetAmount(double netAmount) { this.netAmount = netAmount; }
}
