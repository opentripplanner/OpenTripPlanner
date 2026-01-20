package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.opentripplanner.raptor._data.RaptorTestConstants.D4m;
import static org.opentripplanner.raptor._data.RaptorTestConstants.STOP_A;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;

class RaptorValueTest {

  private static final int C1_VALUE = 1201;
  private static final int C2_VALUE = 101;
  private static final int TX_VALUE = 1;
  private static final int TX_PRIORITY_VALUE = 31;
  private static final int TIME_PENALTY_VALUE = 600;
  private static final int RIDES_VALUE = 12;
  private static final int VIAS_VALUE = 3;
  private static final int WAIT_TIME_COST_VALUE = 102;
  private static final RaptorValue C1 = new RaptorValue(C1_VALUE, RaptorValueType.C1);
  private static final RaptorValue C2 = new RaptorValue(C2_VALUE, RaptorValueType.C2);
  private static final RaptorValue RIDES = new RaptorValue(RIDES_VALUE, RaptorValueType.RIDES);
  private static final RaptorValue TRANSFERS = new RaptorValue(TX_VALUE, RaptorValueType.TRANSFERS);
  private static final RaptorValue TRANSFER_PRIORITY = new RaptorValue(
    TX_PRIORITY_VALUE,
    RaptorValueType.TRANSFER_PRIORITY
  );
  private static final RaptorValue TIME_PENALTY = new RaptorValue(
    TIME_PENALTY_VALUE,
    RaptorValueType.TIME_PENALTY
  );
  private static final RaptorValue VIAS = new RaptorValue(VIAS_VALUE, RaptorValueType.VIAS);
  private static final RaptorValue WAIT_TIME_COST = new RaptorValue(
    WAIT_TIME_COST_VALUE,
    RaptorValueType.WAIT_TIME_COST
  );

  @Test
  void value() {
    assertEquals(C1_VALUE, C1.value());
    assertEquals(C2_VALUE, C2.value());
    assertEquals(RIDES_VALUE, RIDES.value());
    assertEquals(TIME_PENALTY_VALUE, TIME_PENALTY.value());
    assertEquals(TX_VALUE, TRANSFERS.value());
    assertEquals(TX_PRIORITY_VALUE, TRANSFER_PRIORITY.value());
    assertEquals(VIAS_VALUE, VIAS.value());
    assertEquals(WAIT_TIME_COST_VALUE, WAIT_TIME_COST.value());
  }

  @Test
  void type() {
    assertEquals(RaptorValueType.C1, C1.type());
    assertEquals(RaptorValueType.C2, C2.type());
    assertEquals(RaptorValueType.RIDES, RIDES.type());
    assertEquals(RaptorValueType.TIME_PENALTY, TIME_PENALTY.type());
    assertEquals(RaptorValueType.TRANSFERS, TRANSFERS.type());
    assertEquals(RaptorValueType.TRANSFER_PRIORITY, TRANSFER_PRIORITY.type());
    assertEquals(RaptorValueType.VIAS, VIAS.type());
    assertEquals(RaptorValueType.WAIT_TIME_COST, WAIT_TIME_COST.type());
  }

  @Test
  void testEqualsAndHashCode() {
    RaptorValue same = new RaptorValue(C1_VALUE, RaptorValueType.C1);
    RaptorValue otherValue = new RaptorValue(333, RaptorValueType.C1);
    RaptorValue otherType = new RaptorValue(333, RaptorValueType.C1);

    assertEquals(same, C1);
    assertNotEquals(otherType, C1);
    assertNotEquals(otherValue, C1);

    assertEquals(same.hashCode(), C1.hashCode());
    assertNotEquals(otherType.hashCode(), C1.hashCode());
    assertNotEquals(otherValue.hashCode(), C1.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("C₁12.01", C1.toString());
    assertEquals("C₂101", C2.toString());
    assertEquals("Rₙ12", RIDES.toString());
    assertEquals("Pₜ600", TIME_PENALTY.toString());
    assertEquals("Tₙ1", TRANSFERS.toString());
    assertEquals("Tₚ31", TRANSFER_PRIORITY.toString());
    assertEquals("Vₙ3", VIAS.toString());
    assertEquals("Wₜ1.02", WAIT_TIME_COST.toString());
  }

  @Test
  void of() {
    assertEquals(C1, RaptorValue.of("C₁12.01"));
    assertEquals(C2, RaptorValue.of("C₂101"));
    assertEquals(RIDES, RaptorValue.of("Rₙ12"));
    assertEquals(TIME_PENALTY, RaptorValue.of("Pₜ600"));
    assertEquals(TRANSFERS, RaptorValue.of("Tₙ1"));
    assertEquals(TRANSFER_PRIORITY, RaptorValue.of("Tₚ31"));
    assertEquals(VIAS, RaptorValue.of("Vₙ3"));
    assertEquals(WAIT_TIME_COST, RaptorValue.of("Wₜ1.02"));
  }

  @Test
  public void testTestAccessEgress_Parse() {
    var walk = TestAccessEgress.walk(STOP_A, D4m, 1233).withTimePenalty(17);
    var free = TestAccessEgress.free(STOP_A);
    var flex = TestAccessEgress.flex(STOP_A, D4m, 2, 1233).withViaLocationsVisited(2);
    var flex2 = TestAccessEgress.flexAndWalk(STOP_A, D4m, 2, 1233);

    assertEquals(walk, TestAccessEgress.of(walk.toString()));
    assertEquals(free, TestAccessEgress.of(free.toString()));
    assertEquals(flex, TestAccessEgress.of(flex.toString()));
    assertEquals(flex2, TestAccessEgress.of(flex2.toString()));
  }
}
