package org.opentripplanner.transit.raptor._data.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.transfer.TransferConstraint.REGULAR_TRANSFER;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.ACCESS_DURATION;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.ACCESS_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_DETAILED_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.BASIC_PATH_AS_STRING;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.COST_CALCULATOR;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.EGRESS_DURATION;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.L11_DURATION;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.L11_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.L21_DURATION;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.L21_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.L31_DURATION;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.L31_START;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.LINE_11;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.LINE_21;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.LINE_31;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TOTAL_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TRANSIT_RELUCTANCE_INDEX;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TX_COST;
import static org.opentripplanner.transit.raptor._data.stoparrival.BasicPathTestCase.TX_DURATION;
import static org.opentripplanner.util.time.DurationUtils.durationInSeconds;
import static org.opentripplanner.util.time.TimeUtils.time;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;


/**
 * Test the PathBuilder to be sure that it works properly before using it in other tests.
 */
public class TestPathBuilderTest implements RaptorTestConstants {

  private final TestPathBuilder subject = new TestPathBuilder(ALIGHT_SLACK, COST_CALCULATOR);

  @Test
  public void testSimplePathWithOneTransit() {
    int transitDuration = durationInSeconds("5m");

    var path = subject
        .access(time("10:00:15"), D1m, STOP_A)
        .bus("L1", time("10:02"), transitDuration, STOP_B)
        .egress(D2m);

    var transitLeg = path.accessLeg().nextLeg().asTransitLeg();
    int boardCost = COST_CALCULATOR.boardingCost(
            true,
            path.accessLeg().toTime(),
            STOP_A,
            transitLeg.fromTime(),
            transitLeg.trip(),
            REGULAR_TRANSFER
    );

    int transitCost = COST_CALCULATOR.transitArrivalCost(
            boardCost, ALIGHT_SLACK, transitDuration, TRANSIT_RELUCTANCE_INDEX, STOP_B
    );

    int accessEgressCost = TestTransfer.walkCost(D2m + D1m);

    assertEquals(accessEgressCost + transitCost, path.generalizedCost());
    assertEquals(
        "Walk 1m ~ A ~ BUS L1 10:02 10:07 ~ B ~ Walk 2m [10:00:15 10:09:15 9m 0tx $798]",
        path.toString(this::stopIndexToName)
    );
  }

  @Test
  public void testBasicPath() {
    var path = subject
        .access(ACCESS_START, ACCESS_DURATION, STOP_A)
        .bus(LINE_11, L11_START, L11_DURATION, STOP_B)
        .walk(TX_DURATION, STOP_C, TX_COST)
        .bus(LINE_21, L21_START, L21_DURATION, STOP_D)
        .bus(LINE_31, L31_START, L31_DURATION, STOP_E)
        .egress(EGRESS_DURATION);
    assertEquals(BASIC_PATH_AS_STRING, path.toString(this::stopIndexToName));
    assertEquals(BASIC_PATH_AS_DETAILED_STRING, path.toStringDetailed(this::stopIndexToName));
    assertEquals(TOTAL_COST, path.generalizedCost());
  }
}