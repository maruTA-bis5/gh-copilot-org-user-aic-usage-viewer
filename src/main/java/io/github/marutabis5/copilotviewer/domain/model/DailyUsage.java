package io.github.marutabis5.copilotviewer.domain.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated AI credit usage for a single calendar day.
 * May contain multiple {@link UsageItem}s (one per product/SKU/model combination).
 */
public final class DailyUsage {

    private final LocalDate date;
    private final List<UsageItem> items;

    public DailyUsage(LocalDate date, List<UsageItem> items) {
        this.date = Objects.requireNonNull(date, "date must not be null");
        this.items = Collections.unmodifiableList(
                new ArrayList<>(Objects.requireNonNull(items, "items must not be null")));
    }

    public LocalDate getDate() { return date; }

    public List<UsageItem> getItems() { return items; }

    /** Sum of gross credit quantities across all items for this day. */
    public double getTotalGrossQuantity() {
        return items.stream().mapToDouble(UsageItem::getGrossQuantity).sum();
    }

    /** Sum of discount credit quantities across all items for this day. */
    public double getTotalDiscountQuantity() {
        return items.stream().mapToDouble(UsageItem::getDiscountQuantity).sum();
    }

    /** Sum of net amounts (cost) across all items for this day. */
    public double getTotalNetAmount() {
        return items.stream().mapToDouble(UsageItem::getNetAmount).sum();
    }

    /** {@code true} when this day has no recorded usage items. */
    public boolean isEmpty() { return items.isEmpty(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailyUsage that)) return false;
        return Objects.equals(date, that.date) && Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() { return Objects.hash(date, items); }

    @Override
    public String toString() {
        return "DailyUsage{date=%s, items=%d, totalGrossQuantity=%.4f}"
                .formatted(date, items.size(), getTotalGrossQuantity());
    }
}
