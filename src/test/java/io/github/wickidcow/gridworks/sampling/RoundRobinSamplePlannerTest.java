package io.github.wickidcow.gridworks.sampling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RoundRobinSamplePlannerTest {
    @Test
    void initialPopulationIsSpreadAcrossTicksByBudget() {
        RoundRobinSamplePlanner<Object> planner =
                new RoundRobinSamplePlanner<>(10L, 3);

        for (int i = 0; i < 10; i++) {
            planner.add(new Object());
        }

        Set<Object> seen =
                Collections.newSetFromMap(new IdentityHashMap<>());

        for (int tick = 0; tick < 4; tick++) {
            List<Object> batch = planner.nextTickBatch();
            assertTrue(batch.size() <= 3);
            seen.addAll(batch);
        }

        assertEquals(10, seen.size());
    }

    @Test
    void steadyStateHonorsTargetIntervalForSmallPopulation() {
        RoundRobinSamplePlanner<Object> planner =
                new RoundRobinSamplePlanner<>(10L, 64);
        Object sensor = new Object();
        planner.add(sensor);

        assertEquals(List.of(sensor), planner.nextTickBatch());

        int emptyTicks = 0;
        for (int i = 0; i < 9; i++) {
            if (planner.nextTickBatch().isEmpty()) {
                emptyTicks++;
            }
        }
        assertEquals(9, emptyTicks);
        assertEquals(List.of(sensor), planner.nextTickBatch());
    }

    @Test
    void overloadedPopulationNeverExceedsPerTickBudget() {
        RoundRobinSamplePlanner<Object> planner =
                new RoundRobinSamplePlanner<>(10L, 32);

        for (int i = 0; i < 1000; i++) {
            planner.add(new Object());
        }

        for (int tick = 0; tick < 50; tick++) {
            assertTrue(planner.nextTickBatch().size() <= 32);
        }
        assertEquals(32L, planner.estimatedSweepTicks());
    }

    @Test
    void roundRobinEventuallyVisitsEveryMember() {
        RoundRobinSamplePlanner<Object> planner =
                new RoundRobinSamplePlanner<>(20L, 4);
        Set<Object> expected =
                Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> seen =
                Collections.newSetFromMap(new IdentityHashMap<>());

        for (int i = 0; i < 25; i++) {
            Object sensor = new Object();
            expected.add(sensor);
            planner.add(sensor);
        }

        for (int tick = 0; tick < 20 && seen.size() < expected.size(); tick++) {
            seen.addAll(planner.nextTickBatch());
        }

        assertEquals(expected, seen);
    }

    @Test
    void identitySemanticsAllowEqualButDistinctObjects() {
        RoundRobinSamplePlanner<String> planner =
                new RoundRobinSamplePlanner<>(10L, 8);
        String first = new String("sensor");
        String second = new String("sensor");

        assertTrue(planner.add(first));
        assertTrue(planner.add(second));
        assertEquals(2, planner.size());

        assertTrue(planner.remove(first));
        assertEquals(1, planner.size());
        assertFalse(planner.remove(first));

        List<String> batch = planner.nextTickBatch();
        assertEquals(1, batch.size());
        assertTrue(batch.getFirst() == second);
    }
}
