package io.github.wickidcow.gridworks.api.power;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.wickidcow.gridworks.api.control.ControlAddress;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoadSheddingRoutesTest {
    private static final UUID NODE_ID =
            UUID.fromString("12345678-1234-1234-1234-123456789abc");

    @Test
    void keepsAlreadyDistinctRoutesUnchanged() {
        LoadSheddingRoutes routes = LoadSheddingRoutes.repaired(
                NODE_ID,
                new ControlAddress("plant_essential"),
                new ControlAddress("plant_normal"),
                new ControlAddress("plant_optional")
        );

        assertEquals("plant_essential", routes.essential().value());
        assertEquals("plant_normal", routes.normal().value());
        assertEquals("plant_optional", routes.optional().value());
    }

    @Test
    void repairsDuplicatesEvenWhenFirstGeneratedFallbackAlsoCollides() {
        ControlAddress generatedNormal =
                ControlAddress.defaultFor(NODE_ID, "normal");

        LoadSheddingRoutes routes = LoadSheddingRoutes.repaired(
                NODE_ID,
                generatedNormal,
                generatedNormal,
                generatedNormal
        );

        assertEquals(generatedNormal, routes.essential());
        assertNotEquals(routes.essential(), routes.normal());
        assertNotEquals(routes.essential(), routes.optional());
        assertNotEquals(routes.normal(), routes.optional());
    }

    @Test
    void recordRejectsConflictingRoutes() {
        ControlAddress duplicate = new ControlAddress("same");

        assertThrows(
                IllegalArgumentException.class,
                () -> new LoadSheddingRoutes(
                        duplicate,
                        duplicate,
                        new ControlAddress("other")
                )
        );
    }
}
