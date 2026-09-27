package io.github.wickidcow.gridworks.api.power;

public enum LoadSheddingStage {
    NORMAL,
    SHED_OPTIONAL,
    SHED_NORMAL_AND_OPTIONAL;

    public boolean allowsEssentialLoads() {
        return true;
    }

    public boolean allowsNormalLoads() {
        return this != SHED_NORMAL_AND_OPTIONAL;
    }

    public boolean allowsOptionalLoads() {
        return this == NORMAL;
    }
}
