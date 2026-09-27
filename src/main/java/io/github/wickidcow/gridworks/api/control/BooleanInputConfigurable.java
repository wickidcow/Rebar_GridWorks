package io.github.wickidcow.gridworks.api.control;

/**
 * A physical boolean actuator whose accepted command circuit can be selected.
 */
public interface BooleanInputConfigurable {
    BooleanInputMode getBooleanInputMode();

    void setBooleanInputMode(BooleanInputMode mode);
}
