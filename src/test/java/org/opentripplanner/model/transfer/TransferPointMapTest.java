package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.transfer.TransferTestData.ANY_STOP;
import static org.opentripplanner.model.transfer.TransferTestData.POS_1;
import static org.opentripplanner.model.transfer.TransferTestData.POS_2;
import static org.opentripplanner.model.transfer.TransferTestData.POS_3;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1A;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1S;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2B;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2S;
import static org.opentripplanner.model.transfer.TransferTestData.STATION_POINT;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_A;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_B;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_A;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_B;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_11;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_21;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_11_1;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_21_3;

import java.util.List;
import org.junit.jupiter.api.Test;

class TransferPointMapTest {

  private static final int ANY_STOP_POS = 999;

  final TransferPointMap<String> subject = new TransferPointMap<>();

  @Test
  void addAndGetEmptyMap() {
    assertEquals(List.of(), subject.get(TRIP_11, STOP_A, POS_1));
  }

  @Test
  void addAndGet() {
    subject.put(TRIP_POINT_11_1, "A");
    subject.put(TRIP_POINT_21_3, "B");
    subject.put(ROUTE_POINT_1A, "C");
    subject.put(ROUTE_POINT_2B, "D");
    subject.put(ROUTE_POINT_1S, "E");
    subject.put(ROUTE_POINT_2S, "F");
    subject.put(STOP_POINT_A, "G");
    subject.put(STOP_POINT_B, "H");
    subject.put(STATION_POINT, "I");

    assertEquals(List.of("A", "C", "E", "G", "I"), subject.get(TRIP_11, STOP_A, POS_1));
    assertEquals(List.of("F", "G", "I"), subject.get(TRIP_21, STOP_A, ANY_STOP_POS));
    assertEquals(List.of("D", "H"), subject.get(TRIP_21, STOP_B, POS_2));
    assertEquals(List.of("B"), subject.get(TRIP_21, ANY_STOP, POS_3));
  }

  @Test
  void computeIfAbsent() {
    assertEquals("A", subject.computeIfAbsent(TRIP_POINT_11_1, () -> "A"));
    assertEquals("B", subject.computeIfAbsent(ROUTE_POINT_1A, () -> "B"));
    assertEquals("C", subject.computeIfAbsent(STOP_POINT_B, () -> "C"));
    assertEquals("D", subject.computeIfAbsent(STATION_POINT, () -> "D"));
    assertEquals("E", subject.computeIfAbsent(ROUTE_POINT_1S, () -> "E"));

    assertEquals(List.of("A", "B", "E", "D"), subject.get(TRIP_11, STOP_A, POS_1));
    assertEquals(List.of("C"), subject.get(TRIP_21, STOP_B, POS_2));
  }
}
