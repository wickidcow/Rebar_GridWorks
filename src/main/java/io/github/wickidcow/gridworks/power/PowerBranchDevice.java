package io.github.wickidcow.gridworks.power;

import java.util.UUID;

/**
 * Loaded GridWorks device that targets one provider-exposed power branch.
 */
public interface PowerBranchDevice {
    void reconcileBranch();

    void markTargetUnavailable(String reason);

    boolean targetsChunk(UUID worldId, int chunkX, int chunkZ);
}
