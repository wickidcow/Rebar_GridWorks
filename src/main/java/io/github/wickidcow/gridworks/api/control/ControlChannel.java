package io.github.wickidcow.gridworks.api.control;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Namespaced identifier for a control-bus signal, for example
 * {@code gridworks:power/load} or {@code myaddon:machine/running}.
 */
public record ControlChannel(String namespace, String key) {
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("[a-z0-9._-]+");
    private static final Pattern KEY_PATTERN = Pattern.compile("[a-z0-9/._-]+");

    public ControlChannel {
        namespace = validate("namespace", namespace, NAMESPACE_PATTERN);
        key = validate("key", key, KEY_PATTERN);
    }

    public static ControlChannel of(String namespace, String key) {
        return new ControlChannel(namespace, key);
    }

    public static ControlChannel parse(String value) {
        Objects.requireNonNull(value, "value");
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1 || value.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Control channel must use namespace:key format");
        }
        return new ControlChannel(value.substring(0, separator), value.substring(separator + 1));
    }

    private static String validate(String name, String value, Pattern pattern) {
        Objects.requireNonNull(value, name);
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must match " + pattern.pattern());
        }
        return value;
    }

    @Override
    public String toString() {
        return namespace + ':' + key;
    }
}
