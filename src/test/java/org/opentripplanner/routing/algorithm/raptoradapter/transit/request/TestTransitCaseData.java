package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.LocalDate;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;

public final class TestTransitCaseData {

  public static final Station STATION_A = TransitModelForTest
    .station("A")
    .withCoordinate(60.0, 11.1)
    .build();
  public static final Station STATION_B = TransitModelForTest
    .station("B")
    .withCoordinate(61.0, 11.5)
    .build();

  public static final RegularStop STOP_A = TransitModelForTest.stopForTest(
    "A",
    60.0,
    11.0,
    STATION_A
  );
  public static final RegularStop STOP_B = TransitModelForTest.stopForTest(
    "B",
    60.0,
    11.2,
    STATION_B
  );
  public static final RegularStop STOP_C = TransitModelForTest.stopForTest("C", 61.0, 11.4);
  public static final RegularStop STOP_D = TransitModelForTest.stopForTest("D", 61.0, 11.6);

  public static final LocalDate DATE = LocalDate.of(2021, 12, 24);

  public static final int OFFSET = 0;
}
