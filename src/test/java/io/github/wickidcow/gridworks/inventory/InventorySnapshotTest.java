package io.github.wickidcow.gridworks.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class InventorySnapshotTest {
    @Test
    void occupiedRatioUsesSlotCounts() {
        InventorySnapshot snapshot = new InventorySnapshot(true, 120, 3, 6);
        assertEquals(0.5, snapshot.occupiedRatio());
    }

    @Test
    void emptyInventoryHasZeroRatio() {
        InventorySnapshot snapshot = new InventorySnapshot(true, 0, 0, 0);
        assertEquals(0.0, snapshot.occupiedRatio());
    }

    @Test
    void unavailableSnapshotHasNoMeasurements() {
        assertEquals(new InventorySnapshot(false, 0, 0, 0), InventorySnapshot.unavailable());
        assertThrows(
                IllegalArgumentException.class,
                () -> new InventorySnapshot(false, 1, 0, 0)
        );
    }
}
