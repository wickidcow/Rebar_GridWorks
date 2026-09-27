package io.github.wickidcow.gridworks.api.control;

/**
 * A control node that owns one or more current-state signals.
 *
 * <p>GridWorks invokes this after physical topology changes so stateful sensors
 * can replay their current values to newly loaded or newly connected peers
 * without requiring a polling loop.</p>
 */
public interface ControlStateSource extends ControlNode {
    void publishCurrentState();
}
