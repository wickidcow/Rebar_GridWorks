package io.github.wickidcow.gridworks.api.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ControlInputRouteTest {
    private static final ControlAddress ADDRESS =
            new ControlAddress("factory_line");

    @Test
    void defaultsToDefaultCircuit() {
        ControlInputRoute route = ControlInputRoute.defaults(ADDRESS);

        assertEquals(ControlInputRouteMode.CIRCUIT, route.mode());
        assertEquals(ControlCommandChannel.DEFAULT, route.circuit());
        assertEquals(ADDRESS, route.address());
        assertEquals(ControlCommandChannel.DEFAULT.channel(), route.activeChannel());
    }

    @Test
    void storedValuesUseExistingSafeParsers() {
        ControlInputRoute route = ControlInputRoute.fromStored(
                "address",
                "c",
                "ore_line",
                ADDRESS
        );

        assertEquals(ControlInputRouteMode.ADDRESS, route.mode());
        assertEquals(ControlCommandChannel.C, route.circuit());
        assertEquals(new ControlAddress("ore_line"), route.address());
        assertEquals(new ControlAddress("ore_line").channel(), route.activeChannel());

        ControlInputRoute repaired = ControlInputRoute.fromStored(
                "broken",
                "broken",
                "!!!",
                ADDRESS
        );
        assertEquals(ControlInputRouteMode.CIRCUIT, repaired.mode());
        assertEquals(ControlCommandChannel.DEFAULT, repaired.circuit());
        assertEquals(ADDRESS, repaired.address());
    }

    @Test
    void togglingModeChangesTheActiveRoute() {
        ControlInputRoute.RouteChange change =
                ControlInputRoute.defaults(ADDRESS).toggleMode();

        assertTrue(change.changed());
        assertTrue(change.activeRouteChanged());
        assertEquals(ControlInputRouteMode.ADDRESS, change.route().mode());
        assertEquals(ADDRESS.channel(), change.route().activeChannel());
    }

    @Test
    void circuitCyclesOnlyWhenCircuitRouteIsActive() {
        ControlInputRoute circuitRoute = ControlInputRoute.defaults(ADDRESS);
        ControlInputRoute.RouteChange circuitChange =
                circuitRoute.cycleCircuit(1);

        assertTrue(circuitChange.changed());
        assertTrue(circuitChange.activeRouteChanged());
        assertEquals(ControlCommandChannel.A, circuitChange.route().circuit());

        ControlInputRoute addressRoute = circuitRoute.toggleMode().route();
        ControlInputRoute.RouteChange ignored =
                addressRoute.cycleCircuit(1);

        assertFalse(ignored.changed());
        assertFalse(ignored.activeRouteChanged());
        assertEquals(addressRoute, ignored.route());
    }

    @Test
    void addressEditOnlyChangesActiveRouteInAddressMode() {
        ControlAddress next = new ControlAddress("smelter_line");

        ControlInputRoute circuitRoute = ControlInputRoute.defaults(ADDRESS);
        ControlInputRoute.RouteChange inactiveEdit =
                circuitRoute.withAddress(next);
        assertTrue(inactiveEdit.changed());
        assertFalse(inactiveEdit.activeRouteChanged());
        assertEquals(next, inactiveEdit.route().address());
        assertEquals(circuitRoute.activeChannel(), inactiveEdit.route().activeChannel());

        ControlInputRoute addressRoute = circuitRoute.toggleMode().route();
        ControlInputRoute.RouteChange activeEdit =
                addressRoute.withAddress(next);
        assertTrue(activeEdit.changed());
        assertTrue(activeEdit.activeRouteChanged());
        assertEquals(next.channel(), activeEdit.route().activeChannel());
    }

    @Test
    void identicalAddressIsNoOp() {
        ControlInputRoute route = ControlInputRoute.defaults(ADDRESS);

        ControlInputRoute.RouteChange change = route.withAddress(ADDRESS);

        assertFalse(change.changed());
        assertFalse(change.activeRouteChanged());
        assertEquals(route, change.route());
    }
}
