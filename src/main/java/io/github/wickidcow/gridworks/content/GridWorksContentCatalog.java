package io.github.wickidcow.gridworks.content;

import java.util.List;
import java.util.Set;

/**
 * Canonical player-facing GridWorks content identifiers.
 *
 * <p>Every identifier in this catalog must have an item registration, English
 * metadata, and a survival recipe. Block-only distinctions are intentionally
 * kept out of this catalog so the GridWorks Linker can participate equally.</p>
 */
public final class GridWorksContentCatalog {
    public static final List<String> ALL_IDS = List.of(
            "control_interface",
            "alarm_indicator",
            "alarm_console",
            "redstone_sensor",
            "status_light",
            "control_relay",
            "cargo_isolator",
            "addressed_relay",
            "delay_relay",
            "inventory_sensor",
            "machine_sensor",
            "pulse_relay",
            "fluid_sensor",
            "fluid_valve",
            "power_grid_sensor",
            "power_limiter",
            "factory_controller",
            "load_shedding_controller",
            "smart_breaker",
            "factory_monitor",
            "gridworks_linker"
    );

    public static final Set<String> ALL_ID_SET = Set.copyOf(ALL_IDS);

    private GridWorksContentCatalog() {
        throw new AssertionError("Utility class");
    }

    public static void validateCatalog() {
        if (ALL_IDS.size() != ALL_ID_SET.size()) {
            throw new IllegalStateException(
                    "GridWorks content catalog contains duplicate identifiers"
            );
        }

        for (String id : ALL_IDS) {
            if (!id.matches("[a-z0-9_]+")) {
                throw new IllegalStateException(
                        "Invalid GridWorks content identifier: " + id
                );
            }
        }
    }
}
