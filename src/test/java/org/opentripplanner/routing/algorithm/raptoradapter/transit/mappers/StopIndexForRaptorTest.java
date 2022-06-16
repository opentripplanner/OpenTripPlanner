package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopTransferPriority;

public class StopIndexForRaptorTest {

  private final FeedScopedId ANY_ID = TransitModelForTest.id("1");

  private final Station STATION_A = TransitModelForTest
    .station("A")
    .withPriority(StopTransferPriority.DISCOURAGED)
    .build();
  private final Station STATION_B = TransitModelForTest
    .station("B")
    .withPriority(StopTransferPriority.ALLOWED)
    .build();
  private final Station STATION_C = TransitModelForTest
    .station("C")
    .withPriority(StopTransferPriority.RECOMMENDED)
    .build();
  private final Station STATION_D = TransitModelForTest
    .station("D")
    .withPriority(StopTransferPriority.PREFERRED)
    .build();

  private final Stop STOP_0 = TransitModelForTest.stop("ID-" + 1).build();
  private final Stop STOP_1 = TransitModelForTest
    .stop("ID-" + 2)
    .withParentStation(STATION_A)
    .build();
  private final Stop STOP_2 = TransitModelForTest
    .stop("ID-" + 3)
    .withParentStation(STATION_B)
    .build();
  private final Stop STOP_3 = TransitModelForTest
    .stop("ID-" + 4)
    .withParentStation(STATION_C)
    .build();
  private final Stop STOP_4 = TransitModelForTest
    .stop("ID-" + 5)
    .withParentStation(STATION_D)
    .build();

  private final List<StopLocation> STOPS = Arrays.asList(STOP_0, STOP_1, STOP_2, STOP_3, STOP_4);

  @Test
  public void listStopIndexesForEmptyTripPattern() {
    StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);
    var p = new TripPattern(ANY_ID, null, new StopPattern(List.of()));

    int[] result = stopIndex.listStopIndexesForPattern(p);

    assertEquals(result.length, 0);
  }

  @Test
  public void listStopIndexesForTripPattern() {
    var stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);
    var tripPattern = new TripPattern(
      ANY_ID,
      null,
      new StopPattern(stopTimes(STOP_0, STOP_2, STOP_4))
    );

    int[] result = stopIndex.listStopIndexesForPattern(tripPattern);

    assertEquals("[0, 2, 4]", Arrays.toString(result));
  }

  @Test
  public void stopBoardAlightCosts() {
    StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);

    int[] result = stopIndex.stopBoardAlightCosts;

    // Expect cost with Raptor precision
    assertEquals("[6000, 360000, 6000, 2000, 0]", Arrays.toString(result));
  }

  private static List<StopTime> stopTimes(Stop... stops) {
    return Arrays.stream(stops).map(StopIndexForRaptorTest::stopTime).collect(Collectors.toList());
  }

  private static StopTime stopTime(Stop stop) {
    var st = new StopTime();
    st.setStop(stop);
    return st;
  }
}
