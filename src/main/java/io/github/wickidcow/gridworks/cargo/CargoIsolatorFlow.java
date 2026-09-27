package io.github.wickidcow.gridworks.cargo;

public final class CargoIsolatorFlow {
    private CargoIsolatorFlow() {
        throw new AssertionError("Utility class");
    }

    public static int transferRate(boolean open, int configuredRate) {
        if (configuredRate <= 0) {
            throw new IllegalArgumentException(
                    "configuredRate must be greater than zero"
            );
        }
        return open ? configuredRate : 0;
    }

    public static boolean acceptsIncoming(boolean open) {
        return open;
    }
}
