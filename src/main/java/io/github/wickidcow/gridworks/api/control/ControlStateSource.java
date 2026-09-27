package io.github.wickidcow.gridworks.api.control;

/**
 * A control node that owns current state which can be replayed after topology
 * changes.
 *
 * <p>This is intentionally event-driven. GridWorks invokes state replay when
 * loaded control topology changes; implementations should publish their current
 * values immediately without starting their own global polling loop.</p>
 */
public interface ControlStateSource extends ControlNode {
    void publishCurrentState();
}
