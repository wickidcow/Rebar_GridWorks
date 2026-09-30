package io.github.wickidcow.gridworks.inventory;

import io.github.wickidcow.gridworks.api.control.ControlChannel;
import io.github.wickidcow.gridworks.api.control.GridWorksChannels;
import java.util.Locale;

public enum StockMetric {
    ITEM_COUNT(
            "Item Count",
            GridWorksChannels.INVENTORY_ITEMS,
            256.0,
            1024.0,
            64.0,
            512.0,
            9_007_199_254_740_991.0
    ),
    OCCUPIED_RATIO(
            "Occupied Slot Fill",
            GridWorksChannels.INVENTORY_OCCUPIED_RATIO,
            0.25,
            0.75,
            0.05,
            0.25,
            1.0
    );

    private final String displayName;
    private final ControlChannel channel;
    private final double defaultLow;
    private final double defaultHigh;
    private final double step;
    private final double shiftStep;
    private final double max;

    StockMetric(
            String displayName,
            ControlChannel channel,
            double defaultLow,
            double defaultHigh,
            double step,
            double shiftStep,
            double max
    ) {
        this.displayName = displayName;
        this.channel = channel;
        this.defaultLow = defaultLow;
        this.defaultHigh = defaultHigh;
        this.step = step;
        this.shiftStep = shiftStep;
        this.max = max;
    }

    public String displayName() {
        return displayName;
    }

    public ControlChannel channel() {
        return channel;
    }

    public double defaultLow() {
        return defaultLow;
    }

    public double defaultHigh() {
        return defaultHigh;
    }

    public double step() {
        return step;
    }

    public double shiftStep() {
        return shiftStep;
    }

    public double max() {
        return max;
    }

    public StockMetric cycle(int direction) {
        StockMetric[] values = values();
        int stepDirection = direction >= 0 ? 1 : -1;
        return values[Math.floorMod(ordinal() + stepDirection, values.length)];
    }

    public static StockMetric fromStored(String stored) {
        if (stored == null) {
            return ITEM_COUNT;
        }

        try {
            return valueOf(stored.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ITEM_COUNT;
        }
    }
}
