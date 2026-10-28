package io.github.wickidcow.gridworks.content.block;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class TimerControllerContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/io/github/wickidcow/gridworks/content/block/TimerControllerBlock.java");

    @Test
    void blockWiresLifecycleAndFailSafeTaskGuards() throws Exception {
        String code = Files.readString(SOURCE);
        assertTrue(code.contains("extends PhysicalControlNodeBlock"));
        assertTrue(code.contains("BooleanInputConfigurable, ControlStateSource"));
        assertTrue(code.contains("private BukkitTask scheduled;"));
        assertTrue(code.contains("runTaskLater(plugin"));
        assertTrue(code.contains("protected void beforeActivated()"));
        assertTrue(code.contains("protected void beforeDeactivated()"));
        assertTrue(code.contains("protected void beforeRemoved()"));
        assertTrue(code.contains("sendOffBeforeDisconnect()"));
        assertTrue(code.contains("protected void afterDeactivated()"));
        assertTrue(code.contains("protected void afterRemoved()"));
        assertTrue(code.contains("engine.resetAfterLoad()"));
        assertTrue(code.contains("onControlPeerUnavailable"));
        assertTrue(code.contains("runOnServerThreadIfActive"));
    }

    @Test
    void unsupportedInventoryClickTypesCannotEditTimersOrOpenAddressEditor() throws Exception {
        String code = Files.readString(SOURCE);
        int handler = code.indexOf("public void handleClick(");
        int guard = code.indexOf("clickType != ClickType.LEFT && clickType != ClickType.RIGHT", handler);
        int address = code.indexOf("if (\"address\".equals(kind))", handler);
        int switchActions = code.indexOf("switch (kind)", handler);
        assertTrue(handler >= 0 && guard > handler && address > guard && switchActions > guard,
                "Unsupported clicks must return before address editing or mutating timer controls");
        assertTrue(code.contains("clickType != ClickType.SHIFT_LEFT && clickType != ClickType.SHIFT_RIGHT"),
                "Intentional shift-click timing adjustments remain supported");
    }

    @Test
    void persistentSettingsDoNotIncludePendingTaskOrInputBaseline() throws Exception {
        String code = Files.readString(SOURCE);
        assertTrue(code.contains("writeNodeData"));
        assertTrue(code.contains("OUTPUT_ADDRESS_KEY"));
        assertTrue(code.contains("OUTPUT_MODE_KEY"));
        assertTrue(code.contains("INITIAL_KEY"));
        assertTrue(code.contains("ON_KEY"));
        assertTrue(code.contains("OFF_KEY"));
        assertTrue(code.contains("publishTo(old, false)"));
        assertTrue(code.contains("getPhysicalControlNetwork()"));
        assertTrue(code.contains("isLinked(getNodeId(), signal.source())"));
    }
}
