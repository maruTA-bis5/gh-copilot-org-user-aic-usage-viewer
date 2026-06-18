package io.github.marutabis5.copilotviewer.domain.model;

import java.time.LocalDate;
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
                Objects.requireNonNull(items, "items must not be null"));
    }

    public LocalDate getDate() { return date; }

    public List<UsageItem> getItems() { return items; }

    /** Sum of net credit quantities across all items for this day. */
    public double getTotalNetQuantity() {
        return items.stream().mapToDouble(UsageItem::getNetQuantity).sum();
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
        return "DailyUsage{date=%s, items=%d, totalNetQuantity=%.4f}"
                .formatted(date, items.size(), getTotalNetQuantity());
    }
}
