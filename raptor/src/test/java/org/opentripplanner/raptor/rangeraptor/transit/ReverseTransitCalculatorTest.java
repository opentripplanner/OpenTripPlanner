package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flex;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.flexAndWalk;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;

public class ReverseTransitCalculatorTest implements RaptorTestConstants {

  public static final int BOARD_SLACK = D20_s;
  public static final int ALIGHT_SLACK = D10_s;
  public static final int TRANSFER_SLACK = D1_m;

  private static final int STOP = 8;

  private static final RaptorAccessEgress WALK_8_m = TestAccessEgress.walk(STOP, D8_m);
  private static final RaptorAccessEgress FLEX_1x_8_m = flex(STOP, D8_m, 1);
  private static final RaptorAccessEgress FLEX_AND_WALK_1x_8_m = flexAndWalk(STOP, D8_m, 1);

  private final TransitCalculator<TestTripSchedule> subject = new ReverseTransitCalculator<>();

  @Test
  public void latestArrivalTime() {
    // Ignore board slack for reverse search, boardSlack is added to alight times.
    int slackInSeconds = 75;
    TestTripSchedule s = TestTripSchedule.schedule().departures(500).build();
    assertEquals(425, subject.stopArrivalTime(s, 0, slackInSeconds));
  }

  @Test
  void calculateEgressDepartureTime() {
    // No time-shift expected for a regular walking egress
    assertEquals(T00_30, subject.calculateEgressDepartureTime(T00_30, WALK_8_m, TRANSFER_SLACK));
    // Transfers slack should be subtracted(reverse search) if the egress arrive on-board
    assertEquals(
      T00_30 - TRANSFER_SLACK,
      subject.calculateEgressDepartureTime(T00_30, FLEX_1x_8_m, TRANSFER_SLACK)
    );
    // Transfers slack is added if the flex egress arrive by walking as well
    assertEquals(
      T00_30 - TRANSFER_SLACK,
      subject.calculateEgressDepartureTime(T00_30, FLEX_AND_WALK_1x_8_m, TRANSFER_SLACK)
    );
    // No time-shift expected if egress is within opening hours
    assertEquals(
      T00_30,
      subject.calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.walk(STOP, D8_m).openingHours(T00_00, T01_00),
        TRANSFER_SLACK
      )
    );
    // Egress should be time-shifted to the closing opening hours (entrance) plus the duration
    // of the egress (to get to the exit) if the departure time is after.
    assertEquals(
      T00_30 + D5_m,
      subject.calculateEgressDepartureTime(
        T00_40,
        TestAccessEgress.walk(STOP, D5_m).openingHours(T00_10, T00_30),
        TRANSFER_SLACK
      )
    );
    // Egress should be time-shifted to the next opening hours if departure time is after
    // opening hours
    assertEquals(
      T00_30 + D3_m - D24_h,
      subject.calculateEgressDepartureTime(
        T00_00,
        TestAccessEgress.walk(STOP, D3_m).openingHours(T00_10, T00_30),
        TRANSFER_SLACK
      )
    );

    // If egress is are closed (opening hours) then TIME_NOT_SET should be returned
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      subject.calculateEgressDepartureTime(
        T00_30,
        TestAccessEgress.free(STOP).openingHoursClosed(),
        TRANSFER_SLACK
      )
    );
  }

  @Test
  void calculateEgressDepartureTimeWithoutTimeShift() {
    // No time-shift expected for a regular walking egress
    assertEquals(
      T00_30,
      subject.calculateEgressDepartureTimeWithoutTimeShift(T00_30, WALK_8_m, TRANSFER_SLACK)
    );

    // Transfers slack should be subtracted(reverse search) if the egress arrive on-board
    assertEquals(
      T00_30 - TRANSFER_SLACK,
      subject.calculateEgressDepartureTimeWithoutTimeShift(T00_30, FLEX_1x_8_m, TRANSFER_SLACK)
    );

    // Transfers slack is added if the flex egress arrive by walking as well
    assertEquals(
      T00_30 - TRANSFER_SLACK,
      subject.calculateEgressDepartureTimeWithoutTimeShift(
        T00_30,
        FLEX_AND_WALK_1x_8_m,
        TRANSFER_SLACK
      )
    );

    // No time-shift expected if egress is within opening hours
    assertEquals(
      T00_30,
      subject.calculateEgressDepartureTimeWithoutTimeShift(
        T00_30,
        TestAccessEgress.walk(STOP, D8_m).openingHours(T00_00, T01_00),
        TRANSFER_SLACK
      )
    );

    // Egress should NOT be time-shifted to the closing opening hours (entrance) plus the duration
    // of the egress (to get to the exit) if the departure time is after.
    assertEquals(
      T00_40,
      subject.calculateEgressDepartureTimeWithoutTimeShift(
        T00_40,
        TestAccessEgress.walk(STOP, D5_m).openingHours(T00_10, T00_30),
        TRANSFER_SLACK
      )
    );
    // Egress should be time-shifted to the next opening hours if departure time is after
    // opening hours
    assertEquals(
      T00_02,
      subject.calculateEgressDepartureTimeWithoutTimeShift(
        T00_02,
        TestAccessEgress.walk(STOP, D3_m).openingHours(T00_10, T00_30),
        TRANSFER_SLACK
      )
    );

    // If egress is are closed (opening hours) then TIME_NOT_SET should be returned
    assertEquals(
      RaptorConstants.TIME_NOT_SET,
      subject.calculateEgressDepartureTimeWithoutTimeShift(
        T00_30,
        TestAccessEgress.free(STOP).openingHoursClosed(),
        TRANSFER_SLACK
      )
    );
  }
}
