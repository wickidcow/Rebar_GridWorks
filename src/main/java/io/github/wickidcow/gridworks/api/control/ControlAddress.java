package io.github.wickidcow.gridworks.api.control;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Human-readable multicast address for boolean Control Bus commands.
 */
public record ControlAddress(String value) {
    public static final int MAX_LENGTH = 32;
    private static final String KEY_PREFIX = "control/address/";
    private static final Pattern VALID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,31}");

    public ControlAddress {
        Objects.requireNonNull(value, "value");
        if (!VALID.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Control address must match " + VALID.pattern()
            );
        }
    }

    public static ControlAddress fromUserInput(String raw) {
        Objects.requireNonNull(raw, "raw");

        String normalized = raw
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^[._-]+", "")
                .replaceAll("[._-]+$", "");

        if (normalized.length() > MAX_LENGTH) {
            normalized = normalized.substring(0, MAX_LENGTH)
                    .replaceAll("[._-]+$", "");
        }

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "Control address must contain at least one letter or number"
            );
        }

        return new ControlAddress(normalized);
    }

    public static ControlAddress fromStoredOrDefault(
            String stored,
            ControlAddress fallback
    ) {
        Objects.requireNonNull(fallback, "fallback");
        if (stored == null) {
            return fallback;
        }

        try {
            return new ControlAddress(stored);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static ControlAddress defaultFor(UUID nodeId, String prefix) {
        Objects.requireNonNull(nodeId, "nodeId");
        ControlAddress normalizedPrefix = fromUserInput(prefix);
        String id = nodeId.toString().substring(0, 8).toLowerCase(Locale.ROOT);
        return fromUserInput(normalizedPrefix.value + "_" + id);
    }

    public ControlChannel channel() {
        return ControlChannel.of("gridworks", KEY_PREFIX + value);
    }

    public static boolean isAddressedChannel(ControlChannel channel) {
        Objects.requireNonNull(channel, "channel");
        return "gridworks".equals(channel.namespace())
                && channel.key().startsWith(KEY_PREFIX)
                && channel.key().length() > KEY_PREFIX.length();
    }

    public static ControlAddress fromChannel(ControlChannel channel) {
        if (!isAddressedChannel(channel)) {
            throw new IllegalArgumentException("Not an addressed control channel: " + channel);
        }
        return new ControlAddress(channel.key().substring(KEY_PREFIX.length()));
    }
}
