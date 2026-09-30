package io.github.wickidcow.gridworks.api.control;

import java.util.List;
import java.util.Objects;
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

    ControlDispatchResult publish(
            UUID source,
            ControlChannel channel,
            ControlValue value
    );

    /**
     * Publishes several values from one source as one logical snapshot.
     *
     * <p>The default implementation preserves compatibility for external
     * ControlBus implementations by delegating to {@link #publish}. GridWorks'
     * graph implementation overrides this method so topology/recipient
     * resolution occurs once for the entire snapshot.</p>
     */
    default List<ControlDispatchResult> publishBatch(
            UUID source,
            List<ControlPublication> publications
    ) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(publications, "publications");

        return publications.stream()
                .map(publication -> publish(
                        source,
                        publication.channel(),
                        publication.value()
                ))
                .toList();
    }

    void clear();
}
