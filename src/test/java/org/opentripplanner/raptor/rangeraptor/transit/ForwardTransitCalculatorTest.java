package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.time.TimeUtils.hm2time;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;

public class ForwardTransitCalculatorTest implements RaptorTestConstants {

  public static final int T00_31 = hm2time(0, 31);
  public static final int BOARD_SLACK = D20s;
  public static final int ALIGHT_SLACK = D10s;
  public static final int TRANSFER_SLACK = D1m;

  private static final int STOP = 8;

  private static final RaptorAccessEgress WALK_8m = TestAccessEgress.walk(STOP, D8m);
  private static final RaptorAccessEgress FLEX_1x_8m = flex(STOP, D8m, 1);
  private static final RaptorAccessEgress FLEX_AND_WALK_1x_8m = flexAndWalk(STOP, D8m, 1);

  private final TransitCalculator<TestTripSchedule> subject = new ForwardTransitCalculator<>();

  @Test
  public void latestArrivalTime() {
    var s = TestTripSchedule.schedule().arrivals(500).build();
    assertEquals(500, subject.stopArrivalTime(s, 0, 0));
  }

  @Test
  void calculateEgressDepartureTime() {
    // No time-shift expected for a regular walking egress
    assertEquals(T00_30, subject.calculateEgressDepartureTime(T00_30, WALK_8m, TRANSFER_SLACK));
    // Transfers slack should be added if the egress arrive on-board
    assertEquals(
      T00_30 + TRANSFER_SLACK,
      subject.calculateEgressDepartureTime(T00_30, FLEX_1x_8m, TRANSFER_SLACK)
    );
    // Transfers slack is added if the flex egress arrive by walking as well
    assertEquals(
      T00_30 + TRANSFER_SLACK,
      subject.calculateEgressDepartureTime(T00_30, FLEX_AND_WALK_1x_8m, TRANSFER_SLACK)
    );
    // No time-shift expected if egress is within opening hours
    assertEquals(
      T00_30,
      subject.calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.walk(STOP, D8m).openingHours(T00_00, T01_00),
        TRANSFER_SLACK
      )
    );
    // Egress should be time-shifted to the opening hours if departure time is before
    assertEquals(
      T00_30,
      subject.calculateEgressDepartureTime(
        T00_10,
        TestAccessEgress.walk(STOP, D8m).openingHours(T00_30, T01_00),
        TRANSFER_SLACK
      )
    );
    // Egress should be time-shifted to the next opening hours if departure time is after
    // opening hours
    assertEquals(
      T00_10 + D24h,
      subject.calculateEgressDepartureTime(
        T00_31,
        TestAccessEgress.walk(STOP, D8m).openingHours(T00_10, T00_30),
        TRANSFER_SLACK
      )
    );

    // If egress is are closed (opening hours) then -1 should be returned
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      subject.calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.walk(STOP, D8m).openingHours(5, 4),
        TRANSFER_SLACK
      )
    );
  }
}
