package io.github.wickidcow.gridworks.alarm;

import java.util.Objects;
import java.util.UUID;

public final class AlarmAcknowledgeRequest {
    public static final String ALL = "*";

    private AlarmAcknowledgeRequest() {
        throw new AssertionError("Utility class");
    }

    public static String target(UUID nodeId) {
        return Objects.requireNonNull(nodeId, "nodeId").toString();
    }

    public static boolean matches(String request, UUID nodeId) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(nodeId, "nodeId");

        return ALL.equals(request) || nodeId.toString().equalsIgnoreCase(request.trim());
    }
}
