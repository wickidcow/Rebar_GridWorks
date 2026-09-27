package io.github.wickidcow.gridworks.api.control;

import java.util.Objects;

/**
 * Immutable routing state for a boolean actuator that can listen to either a
 * compact command circuit or a human-readable addressed command.
 */
public record ControlInputRoute(
        ControlInputRouteMode mode,
        ControlCommandChannel circuit,
        ControlAddress address
) {
    public ControlInputRoute {
        mode = Objects.requireNonNull(mode, "mode");
        circuit = Objects.requireNonNull(circuit, "circuit");
        address = Objects.requireNonNull(address, "address");
    }

    public static ControlInputRoute defaults(ControlAddress address) {
        return new ControlInputRoute(
                ControlInputRouteMode.CIRCUIT,
                ControlCommandChannel.DEFAULT,
                address
        );
    }

    public static ControlInputRoute fromStored(
            String storedMode,
            String storedCircuit,
            String storedAddress,
            ControlAddress fallbackAddress
    ) {
        return new ControlInputRoute(
                ControlInputRouteMode.fromStored(storedMode),
                ControlCommandChannel.fromStored(storedCircuit),
                ControlAddress.fromStoredOrDefault(
                        storedAddress,
                        fallbackAddress
                )
        );
    }

    public ControlChannel activeChannel() {
        return mode == ControlInputRouteMode.ADDRESS
                ? address.channel()
                : circuit.channel();
    }

    public RouteChange toggleMode() {
        return changed(new ControlInputRoute(mode.toggle(), circuit, address));
    }

    public RouteChange cycleCircuit(int direction) {
        if (mode != ControlInputRouteMode.CIRCUIT) {
            return RouteChange.unchanged(this);
        }

        ControlCommandChannel next = circuit.cycle(direction);
        if (next == circuit) {
            return RouteChange.unchanged(this);
        }

        return changed(new ControlInputRoute(mode, next, address));
    }

    public RouteChange withAddress(ControlAddress nextAddress) {
        Objects.requireNonNull(nextAddress, "nextAddress");
        if (address.equals(nextAddress)) {
            return RouteChange.unchanged(this);
        }

        ControlInputRoute next = new ControlInputRoute(
                mode,
                circuit,
                nextAddress
        );
        return new RouteChange(
                next,
                true,
                mode == ControlInputRouteMode.ADDRESS
        );
    }

    private RouteChange changed(ControlInputRoute next) {
        return new RouteChange(next, true, true);
    }

    public record RouteChange(
            ControlInputRoute route,
            boolean changed,
            boolean activeRouteChanged
    ) {
        public RouteChange {
            route = Objects.requireNonNull(route, "route");
            if (activeRouteChanged && !changed) {
                throw new IllegalArgumentException(
                        "activeRouteChanged requires changed=true"
                );
            }
        }

        private static RouteChange unchanged(ControlInputRoute route) {
            return new RouteChange(route, false, false);
        }
    }
}
