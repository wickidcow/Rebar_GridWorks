package io.github.wickidcow.gridworks.api.power;

import java.util.Objects;

public record PowerBranchControlResult(Status status, String message) {
    public PowerBranchControlResult {
        status = Objects.requireNonNull(status, "status");
        message = Objects.requireNonNull(message, "message").trim();
    }

    public boolean applied() {
        return status == Status.APPLIED;
    }

    public static PowerBranchControlResult applied(String message) {
        return new PowerBranchControlResult(Status.APPLIED, message);
    }

    public static PowerBranchControlResult targetNotFound(String message) {
        return new PowerBranchControlResult(Status.TARGET_NOT_FOUND, message);
    }

    public static PowerBranchControlResult unsupported(String message) {
        return new PowerBranchControlResult(Status.UNSUPPORTED, message);
    }

    public static PowerBranchControlResult rejected(String message) {
        return new PowerBranchControlResult(Status.REJECTED, message);
    }

    public enum Status {
        APPLIED,
        TARGET_NOT_FOUND,
        UNSUPPORTED,
        REJECTED
    }
}
