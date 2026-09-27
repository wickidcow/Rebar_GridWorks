package io.github.wickidcow.gridworks.api.power;

import java.util.Objects;

public record LoadSheddingOutputState(
        boolean essentialEnabled,
        boolean normalEnabled,
        boolean optionalEnabled
) {
    public static LoadSheddingOutputState fromStage(LoadSheddingStage stage) {
        Objects.requireNonNull(stage, "stage");
        return new LoadSheddingOutputState(
                stage.allowsEssentialLoads(),
                stage.allowsNormalLoads(),
                stage.allowsOptionalLoads()
        );
    }
}
