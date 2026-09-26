package io.github.wickidcow.gridworks.api.control;

import java.util.Set;
import java.util.UUID;

/**
 * Public GridWorks control-bus service.
 *
 * <p>Implementations are responsible for suppressing duplicate delivery caused
 * by graph cycles and for bounding propagation work.</p>
 */
public interface ControlBus {
    void register(ControlNode node);

    boolean unregister(UUID nodeId);

    void connect(UUID first, UUID second);

    boolean disconnect(UUID first, UUID second);

    boolean isConnected(UUID first, UUID second);

    Set<UUID> neighbors(UUID nodeId);

    Set<UUID> componentOf(UUID nodeId);

    int nodeCount();

    int connectionCount();

    ControlDispatchResult publish(UUID source, ControlChannel channel, ControlValue value);

    void clear();
}
