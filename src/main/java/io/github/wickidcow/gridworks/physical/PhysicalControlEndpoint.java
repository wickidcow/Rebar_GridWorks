package io.github.wickidcow.gridworks.physical;

import io.github.wickidcow.gridworks.api.control.ControlNode;
import java.util.UUID;

/**
 * A physical Control Bus endpoint that can react when a persisted, linked peer
 * becomes available in the live graph.
 */
public interface PhysicalControlEndpoint extends ControlNode {
    default void onControlPeerAvailable(UUID peerId) {
    }
}
