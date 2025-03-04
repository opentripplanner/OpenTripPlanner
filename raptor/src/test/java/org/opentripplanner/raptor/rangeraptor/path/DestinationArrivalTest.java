package org.opentripplanner.raptor.rangeraptor.path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.c1.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1.PatternRideC1;

public class DestinationArrivalTest {

  private static final int ANY = 9_999;
  private static final int ACCESS_STOP = 100;
  private static final int ACCESS_DEPARTURE_TIME = 8 * 60 * 60;
  private static final int ACCESS_DURATION = 72;
  private static final TestAccessEgress ACCESS_WALK = TestAccessEgress.walk(
    ACCESS_STOP,
    ACCESS_DURATION
  );
  private static final int ACCESS_COST = ACCESS_WALK.c1();

  private static final int TRANSIT_STOP = 101;
  private static final int TRANSIT_BOARD_TIME = ACCESS_DEPARTURE_TIME + 10 * 60;
  private static final int TRANSIT_ALIGHT_TIME = TRANSIT_BOARD_TIME + 4 * 60;
  private static final RaptorTripSchedule A_TRIP = null;
  private static final int TRANSIT_COST = 84000;

  private static final int DESTINATION_DURATION_TIME = 50;
  private static final int DESTINATION_C1 = 50000;
  private static final int DESTINATION_C2 = 5;

  private static final int EXPECTED_ARRIVAL_TIME = TRANSIT_ALIGHT_TIME + DESTINATION_DURATION_TIME;
  private static final int EXPECTED_TOTAL_COST = ACCESS_COST + TRANSIT_COST + DESTINATION_C1;

  private static final StopArrivalFactoryC1<RaptorTripSchedule> STOP_ARRIVAL_FACTORY =
    new StopArrivalFactoryC1<RaptorTripSchedule>();

  /**
   * Setup a simple journey with an access leg, one transit and a egress leg.
   */
  private static final McStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL =
    STOP_ARRIVAL_FACTORY.createAccessStopArrival(ACCESS_DEPARTURE_TIME, ACCESS_WALK);

  private static final ArrivalView<RaptorTripSchedule> TRANSIT_ARRIVAL =
    STOP_ARRIVAL_FACTORY.createTransitStopArrival(
      new PatternRideC1<>(ACCESS_ARRIVAL, ANY, ANY, ANY, ANY, ANY, ANY, A_TRIP),
      TRANSIT_STOP,
      TRANSIT_ALIGHT_TIME,
      ACCESS_ARRIVAL.c1() + TRANSIT_COST
    );

  private final DestinationArrival<RaptorTripSchedule> subject = new DestinationArrival<>(
    TestAccessEgress.walk(TRANSIT_STOP, DESTINATION_DURATION_TIME),
    TRANSIT_ARRIVAL,
    TRANSIT_ALIGHT_TIME + DESTINATION_DURATION_TIME,
    DESTINATION_C1,
    DESTINATION_C2
  );

  @Test
  public void arrivalTime() {
    assertEquals(EXPECTED_ARRIVAL_TIME, subject.arrivalTime());
  }

  @Test
  public void cost() {
    assertEquals(EXPECTED_TOTAL_COST, subject.c1());
  }

  @Test
  public void round() {
    assertEquals(1, subject.round());
  }

  @Test
  public void previous() {
    assertSame(TRANSIT_ARRIVAL, subject.previous());
  }

  @Test
  public void testToString() {
    assertEquals(
      "Egress { round: 1, from-stop: 101, arrival: [8:14:50 C₁1_484 C₂5], path: Walk 50s C₁100 ~ 101 }",
      subject.toString()
    );
  }
}
