package io.github.wickidcow.gridworks.inventory;

public record InventorySnapshot(
        boolean available,
        long items,
        int occupiedSlots,
        int totalSlots
) {
    public InventorySnapshot {
        if (items < 0) {
            throw new IllegalArgumentException("items must be non-negative");
        }
        if (occupiedSlots < 0) {
            throw new IllegalArgumentException("occupiedSlots must be non-negative");
        }
        if (totalSlots < 0 || occupiedSlots > totalSlots) {
            throw new IllegalArgumentException("totalSlots must be >= occupiedSlots");
        }
        if (!available && (items != 0 || occupiedSlots != 0 || totalSlots != 0)) {
            throw new IllegalArgumentException("unavailable snapshots must contain zero measurements");
        }
    }

    public static InventorySnapshot unavailable() {
        return new InventorySnapshot(false, 0, 0, 0);
    }

    public double occupiedRatio() {
        return totalSlots == 0 ? 0.0 : (double) occupiedSlots / totalSlots;
    }
}
