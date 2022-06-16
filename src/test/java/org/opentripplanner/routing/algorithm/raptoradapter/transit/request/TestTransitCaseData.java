package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.LocalDate;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;

public final class TestTransitCaseData {

  public static final Station STATION_A = TransitModelForTest
    .station("A")
    .withCoordinate(60.0, 11.1)
    .build();
  public static final Station STATION_B = TransitModelForTest
    .station("B")
    .withCoordinate(61.0, 11.5)
    .build();

  public static final Stop STOP_A = TransitModelForTest.stopForTest("A", 60.0, 11.0, STATION_A);
  public static final Stop STOP_B = TransitModelForTest.stopForTest("B", 60.0, 11.2, STATION_B);
  public static final Stop STOP_C = TransitModelForTest.stopForTest("C", 61.0, 11.4);
  public static final Stop STOP_D = TransitModelForTest.stopForTest("D", 61.0, 11.6);

  // Random order stop indexes - should be different from stopPos in pattern to
  // make sure code-under-test do not mix stopIndex and stopPosition
  public static final Stop[] RAPTOR_STOP_INDEX = { STOP_D, STOP_A, STOP_C, STOP_B };

  public static final LocalDate DATE = LocalDate.of(2021, 12, 24);

  public static final int OFFSET = 0;

  public static int stopIndex(StopLocation stop) {
    for (int i = 0; i < RAPTOR_STOP_INDEX.length; ++i) {
      if (stop == RAPTOR_STOP_INDEX[i]) {
        return i;
      }
    }
    throw new IllegalArgumentException();
  }
}
