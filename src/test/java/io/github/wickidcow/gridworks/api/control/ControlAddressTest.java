package io.github.wickidcow.gridworks.api.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ControlAddressTest {
    @Test
    void normalizesPlayerFriendlyInput() {
        ControlAddress address = ControlAddress.fromUserInput(" Ore Line #1 ");

        assertEquals("ore_line_1", address.value());
        assertEquals(
                ControlChannel.of("gridworks", "control/address/ore_line_1"),
                address.channel()
        );
    }

    @Test
    void recognizesAndRoundTripsAddressedChannels() {
        ControlAddress address = new ControlAddress("cooling_pumps");

        assertTrue(ControlAddress.isAddressedChannel(address.channel()));
        assertEquals(address, ControlAddress.fromChannel(address.channel()));
        assertFalse(ControlAddress.isAddressedChannel(GridWorksChannels.CONTROL_A));
    }

    @Test
    void rejectsBlankInputAndFallsBackForBadStoredValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ControlAddress.fromUserInput("!!!")
        );

        ControlAddress fallback = new ControlAddress("safe");
        assertEquals(
                fallback,
                ControlAddress.fromStoredOrDefault("INVALID VALUE", fallback)
        );
    }

    @Test
    void generatedDefaultsAreStableAndValid() {
        UUID id = UUID.fromString("12345678-1234-1234-1234-123456789abc");

        assertEquals(
                "relay_12345678",
                ControlAddress.defaultFor(id, "relay").value()
        );
    }

    @Test
    void outputModeDefaultsSafely() {
        assertEquals(ControlOutputMode.CIRCUIT, ControlOutputMode.fromStored(null));
        assertEquals(ControlOutputMode.CIRCUIT, ControlOutputMode.fromStored("bad"));
        assertEquals(ControlOutputMode.ADDRESS, ControlOutputMode.CIRCUIT.toggle());
        assertEquals(ControlOutputMode.CIRCUIT, ControlOutputMode.ADDRESS.toggle());
    }
}
