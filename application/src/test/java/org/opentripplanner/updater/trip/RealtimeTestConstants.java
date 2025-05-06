package org.opentripplanner.updater.trip;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;

public final class RealtimeTestConstants {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
  public final LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 8);
  public final String STOP_A1_ID = "A1";
  public final String STOP_B1_ID = "B1";
  public final String STOP_C1_ID = "C1";

  public final ZoneId TIME_ZONE = ZoneId.of(TimetableRepositoryForTest.TIME_ZONE_ID);
  public final Station STATION_A = testModel.station("A").build();
  public final Station STATION_B = testModel.station("B").build();
  public final Station STATION_C = testModel.station("C").build();
  public final Station STATION_D = testModel.station("D").build();
  public final RegularStop STOP_A1 = testModel
    .stop(STOP_A1_ID)
    .withParentStation(STATION_A)
    .build();
  public final RegularStop STOP_B1 = testModel
    .stop(STOP_B1_ID)
    .withParentStation(STATION_B)
    .build();
  public final RegularStop STOP_B2 = testModel.stop("B2").withParentStation(STATION_B).build();
  public final RegularStop STOP_C1 = testModel
    .stop(STOP_C1_ID)
    .withParentStation(STATION_C)
    .build();
  public final RegularStop STOP_D1 = testModel.stop("D1").withParentStation(STATION_D).build();
  public final SiteRepository SITE_REPOSITORY = testModel
    .siteRepositoryBuilder()
    .withRegularStop(STOP_A1)
    .withRegularStop(STOP_B1)
    .withRegularStop(STOP_B2)
    .withRegularStop(STOP_C1)
    .withRegularStop(STOP_D1)
    .build();
}
