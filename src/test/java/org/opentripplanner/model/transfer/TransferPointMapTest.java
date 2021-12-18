package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransferPointMapTest implements TransferTestData {

    final TransferPointMap<String> subject = new TransferPointMap<>();

    @BeforeEach
    void setup() {
        STOP_A.setParentStation(STATION);
    }

    @Test
    void addAndGetEmptyMap() {
        assertEquals(List.of(), subject.get(TRIP_1, STOP_A, STOP_POSITION_1));
    }

    @Test
    void addAndGet() {
        subject.put(TRIP_POINT_11, "A");
        subject.put(TRIP_POINT_23, "B");
        subject.put(ROUTE_POINT_11, "C");
        subject.put(ROUTE_POINT_22, "D");
        subject.put(STOP_POINT_A, "E");
        subject.put(STOP_POINT_B, "F");
        subject.put(STATION_POINT, "G");

        assertEquals(List.of("A", "C", "E", "G"), subject.get(TRIP_1, STOP_A, STOP_POSITION_1));
        assertEquals(List.of("D", "F"), subject.get(TRIP_2, STOP_B, STOP_POSITION_2));
    }

    @Test
    void computeIfAbsent() {
        assertEquals("A", subject.computeIfAbsent(TRIP_POINT_11, () -> "A"));
        assertEquals("B", subject.computeIfAbsent(ROUTE_POINT_11, () -> "B"));
        assertEquals("C", subject.computeIfAbsent(STOP_POINT_B, () -> "C"));
        assertEquals("D", subject.computeIfAbsent(STATION_POINT, () -> "D"));
        assertEquals("B", subject.computeIfAbsent(ROUTE_POINT_11, () -> "E"));

        assertEquals(List.of("A", "B", "D"), subject.get(TRIP_1, STOP_A, STOP_POSITION_1));
        assertEquals(List.of("C"), subject.get(TRIP_2, STOP_B, STOP_POSITION_2));
    }
}