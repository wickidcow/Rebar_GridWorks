package io.github.wickidcow.gridworks.fluid;

public final class FluidValveFlow {
    private FluidValveFlow() {
        throw new AssertionError("Utility class");
    }

    public static double requestedAmount(
            boolean open,
            boolean compatibleFluid,
            double spaceRemaining
    ) {
        requireNonNegativeFinite(spaceRemaining, "spaceRemaining");
        return open && compatibleFluid ? spaceRemaining : 0.0;
    }

    public static double suppliedAmount(boolean open, double storedAmount) {
        requireNonNegativeFinite(storedAmount, "storedAmount");
        return open ? storedAmount : 0.0;
    }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(
                    name + " must be finite and non-negative"
            );
        }
    }
}
