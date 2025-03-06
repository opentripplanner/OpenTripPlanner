package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.time.LocalDate;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;

public final class TestTransitCaseData {

  private static TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  public static final Station STATION_A = TEST_MODEL.station("A")
    .withCoordinate(60.0, 11.1)
    .build();
  public static final Station STATION_B = TEST_MODEL.station("B")
    .withCoordinate(61.0, 11.5)
    .build();

  public static final RegularStop STOP_A = TEST_MODEL.stop("A", 60.0, 11.0)
    .withParentStation(STATION_A)
    .build();
  public static final RegularStop STOP_B = TEST_MODEL.stop("B", 60.0, 11.2)
    .withParentStation(STATION_B)
    .build();
  public static final RegularStop STOP_C = TEST_MODEL.stop("C", 61.0, 11.4).build();
  public static final RegularStop STOP_D = TEST_MODEL.stop("D", 61.0, 11.6).build();

  public static final LocalDate DATE = LocalDate.of(2021, 12, 24);

  public static final int OFFSET = 0;
}
