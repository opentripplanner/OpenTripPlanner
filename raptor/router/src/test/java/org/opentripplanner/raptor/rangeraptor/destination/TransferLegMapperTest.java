package org.opentripplanner.raptor.rangeraptor.destination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.access;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.slackedTransfer;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.transfer;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.transferWithoutC1;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.viaTransfer;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestCostCalculator;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.path.PathBuilder;
import org.opentripplanner.raptor.path.PathBuilderLeg;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

class TransferLegMapperTest implements RaptorTestConstants {

  private static final int START = 10 * 3600;
  private static final int DURATION = 120;
  // The leg has no times until the path is built, the duration is part of the leg text
  private static final String WALK_TWO_MINUTES = "Walk 2m ~ " + STOP_B;
  private static final RaptorCostCalculator<TestTripSchedule> CALCULATOR = new TestCostCalculator(
    0,
    0,
    1.0,
    null
  );

  private final TestTransitData transitData = new TestTransitData();
  private final ArrivalView<TestTripSchedule> previous = access(STOP_A, START - 60, START, 100, 0);

  @Test
  void durationAndC1AreTakenFromTheArrivalsWhenC1IsPartOfTheState() {
    // The transfer data is empty, so a lookup would fail
    var arrival = transfer(1, STOP_B, START, START + DURATION, 500, previous);

    var leg = map(arrival);

    assertTrue(leg.isTransfer());
    assertEquals(STOP_B, leg.toStop());
    assertEquals(WALK_TWO_MINUTES, leg.toString());
    assertEquals(500, c1(leg));
  }

  @Test
  void c1IsLookedUpWhenItIsNotPartOfTheState() {
    transitData
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 100, 700))
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, DURATION, 400))
      // Too long, even if cheaper
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, DURATION + 1, 100))
      // Other direction and other stop
      .withTransfer(STOP_B, TestTransfer.transfer(STOP_A, DURATION, 50))
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_C, DURATION, 10));
    var arrival = transferWithoutC1(
      1,
      START + DURATION,
      TestTransfer.transfer(STOP_B, DURATION, 999),
      previous
    );

    var leg = map(arrival);

    assertEquals(WALK_TWO_MINUTES, leg.toString());
    assertEquals(400, c1(leg));
  }

  @Test
  void theLookupFailsIfNoTransferMatches() {
    transitData.withTransfer(STOP_A, TestTransfer.transfer(STOP_B, DURATION + 1, 400));
    var arrival = transferWithoutC1(
      1,
      START + DURATION,
      TestTransfer.transfer(STOP_B, DURATION, 400),
      previous
    );

    assertThrows(IllegalStateException.class, () -> map(arrival));
  }

  @Test
  void aViaVisitDoesNotIncludeTheWaitTimeInTheTransferDuration() {
    var viaWaitTime = 300;
    // The arrival time includes the wait time of the via visit
    var arrival = slackedTransfer(
      1,
      START + DURATION + viaWaitTime,
      TestTransfer.transfer(STOP_B, DURATION, 500),
      viaWaitTime,
      previous
    );

    var leg = map(arrival);

    assertEquals(DURATION + viaWaitTime, arrival.hopDuration());
    assertEquals(WALK_TWO_MINUTES, leg.toString());
    assertEquals(500, c1(leg));
  }

  @Test
  void aViaTransferIsKeptAsIsAndDoesNotIncludeTheWaitTime() {
    var viaTx = TestTransfer.transfer(STOP_B, DURATION, 400);
    var minWaitTime = 300;
    var arrival = viaTransfer(1, START + DURATION + minWaitTime, viaTx, previous);

    var leg = map(arrival);

    assertEquals(DURATION + minWaitTime, arrival.hopDuration());
    assertEquals(WALK_TWO_MINUTES, leg.toString());
    assertEquals(400, c1(leg));
    assertSame(viaTx, arrival.viaTransfer());
  }

  private PathBuilderLeg<TestTripSchedule> map(ArrivalView<TestTripSchedule> arrival) {
    var mapper = new TransferLegMapper<>(transitData);
    var builder = PathBuilder.tailPathBuilder(SLACK_PROVIDER, START, CALCULATOR, null, null);
    mapper.addTransfer(builder, arrival, STOP_A, STOP_B);
    return builder.head();
  }

  private static int c1(PathBuilderLeg<TestTripSchedule> leg) {
    return leg.c1(CALCULATOR, SLACK_PROVIDER);
  }
}
