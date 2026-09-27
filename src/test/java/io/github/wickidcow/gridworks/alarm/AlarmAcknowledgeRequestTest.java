package io.github.wickidcow.gridworks.alarm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlarmAcknowledgeRequestTest {
    @Test
    void matchesExactTargetAndWildcard() {
        UUID target = UUID.randomUUID();

        assertTrue(AlarmAcknowledgeRequest.matches(
                AlarmAcknowledgeRequest.target(target),
                target
        ));
        assertTrue(AlarmAcknowledgeRequest.matches(AlarmAcknowledgeRequest.ALL, target));
        assertFalse(AlarmAcknowledgeRequest.matches(
                UUID.randomUUID().toString(),
                target
        ));
    }
}
