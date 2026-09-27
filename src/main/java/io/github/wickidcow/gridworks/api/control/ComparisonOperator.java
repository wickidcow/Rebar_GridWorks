package io.github.wickidcow.gridworks.api.control;

public enum ComparisonOperator {
    GREATER_THAN(">") {
        @Override
        public boolean test(double observed, double threshold) {
            return observed > threshold;
        }
    },
    GREATER_OR_EQUAL(">=") {
        @Override
        public boolean test(double observed, double threshold) {
            return observed >= threshold;
        }
    },
    LESS_THAN("<") {
        @Override
        public boolean test(double observed, double threshold) {
            return observed < threshold;
        }
    },
    LESS_OR_EQUAL("<=") {
        @Override
        public boolean test(double observed, double threshold) {
            return observed <= threshold;
        }
    },
    EQUAL("==") {
        @Override
        public boolean test(double observed, double threshold) {
            return Double.compare(observed, threshold) == 0;
        }
    },
    NOT_EQUAL("!=") {
        @Override
        public boolean test(double observed, double threshold) {
            return Double.compare(observed, threshold) != 0;
        }
    };

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public abstract boolean test(double observed, double threshold);

    public String symbol() {
        return symbol;
    }
}
