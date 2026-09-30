package io.github.wickidcow.gridworks.content.guide;

import io.github.wickidcow.gridworks.content.GridWorksContentCatalog;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GridWorksGuideCatalog {
    public static final String ROOT_PAGE_KEY = "gridworks";

    public static final List<Category> CATEGORIES = List.of(
            new Category("core_linking", List.of("control_interface", "gridworks_linker")),
            new Category("sensors", List.of(
                    "redstone_sensor",
                    "inventory_sensor",
                    "machine_sensor",
                    "fluid_sensor",
                    "power_grid_sensor"
            )),
            new Category("logic_production", List.of(
                    "factory_controller",
                    "batch_controller",
                    "sequence_controller",
                    "load_shedding_controller",
                    "stock_controller",
                    "delay_relay",
                    "pulse_relay"
            )),
            new Category("monitoring_alarms", List.of(
                    "status_light",
                    "alarm_indicator",
                    "alarm_console",
                    "factory_monitor"
            )),
            new Category("actuators_power", List.of(
                    "control_relay",
                    "addressed_relay",
                    "cargo_isolator",
                    "fluid_valve",
                    "smart_breaker",
                    "power_limiter"
            ))
    );

    private GridWorksGuideCatalog() {
        throw new AssertionError("Utility class");
    }

    public static void validateCatalog() {
        Set<String> categoryKeys = new LinkedHashSet<>();
        Set<String> itemIds = new LinkedHashSet<>();

        for (Category category : CATEGORIES) {
            if (!category.key().matches("[a-z0-9_]+")) {
                throw new IllegalStateException(
                        "Invalid GridWorks guide category key: " + category.key()
                );
            }
            if (!categoryKeys.add(category.key())) {
                throw new IllegalStateException(
                        "Duplicate GridWorks guide category: " + category.key()
                );
            }
            if (category.itemIds().isEmpty()) {
                throw new IllegalStateException(
                        "GridWorks guide category is empty: " + category.key()
                );
            }

            for (String itemId : category.itemIds()) {
                if (!itemIds.add(itemId)) {
                    throw new IllegalStateException(
                            "GridWorks guide item appears in multiple categories: " + itemId
                    );
                }
            }
        }

        if (!itemIds.equals(GridWorksContentCatalog.ALL_ID_SET)) {
            Set<String> missing = new LinkedHashSet<>(GridWorksContentCatalog.ALL_ID_SET);
            missing.removeAll(itemIds);

            Set<String> unknown = new LinkedHashSet<>(itemIds);
            unknown.removeAll(GridWorksContentCatalog.ALL_ID_SET);

            throw new IllegalStateException(
                    "GridWorks guide catalog does not match content catalog"
                            + " (missing=" + missing
                            + ", unknown=" + unknown + ")"
            );
        }
    }

    public record Category(String key, List<String> itemIds) {
        public Category {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("category key must not be blank");
            }
            itemIds = List.copyOf(itemIds);
        }
    }
}
