package io.github.wickidcow.gridworks.control;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DelayedBooleanTransitionTest {
    @Test
    void schedulesOnlyWhenDesiredStateDiffersFromOutput() {
        DelayedBooleanTransition transition = new DelayedBooleanTransition(false);

        assertEquals(
                DelayedBooleanTransition.Action.NONE,
                transition.observe(false)
        );
        assertEquals(
                DelayedBooleanTransition.Action.SCHEDULE_ON,
                transition.observe(true)
        );
        assertEquals(
                DelayedBooleanTransition.Action.NONE,
                transition.observe(true)
        );

        assertTrue(transition.commit(true));
        assertTrue(transition.output());
        assertNull(transition.pendingTarget());
    }

    @Test
    void reversingInputCancelsPendingTransition() {
        DelayedBooleanTransition transition = new DelayedBooleanTransition(false);

        assertEquals(
                DelayedBooleanTransition.Action.SCHEDULE_ON,
                transition.observe(true)
        );
        assertEquals(
                DelayedBooleanTransition.Action.CANCEL_PENDING,
                transition.observe(false)
        );

        assertFalse(transition.commit(true));
        assertFalse(transition.output());
        assertNull(transition.pendingTarget());
    }

    @Test
    void canReplacePendingTargetAfterOutputChanges() {
        DelayedBooleanTransition transition = new DelayedBooleanTransition(false);

        transition.observe(true);
        assertTrue(transition.commit(true));

        assertEquals(
                DelayedBooleanTransition.Action.SCHEDULE_OFF,
                transition.observe(false)
        );
        assertEquals(Boolean.FALSE, transition.pendingTarget());
        assertTrue(transition.commit(false));
        assertFalse(transition.output());
    }

    @Test
    void staleScheduledCommitIsIgnored() {
        DelayedBooleanTransition transition = new DelayedBooleanTransition(false);

        transition.observe(true);
        transition.observe(false);

        assertFalse(transition.commit(true));
        assertFalse(transition.output());
    }
}
