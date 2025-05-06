package org.opentripplanner.updater.trip;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

public interface RealtimeTestConstants {
  LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 8);
  ZoneId TIME_ZONE = ZoneId.of(TimetableRepositoryForTest.TIME_ZONE_ID);

  String TRIP_1_ID = "TestTrip1";
  String TRIP_2_ID = "TestTrip2";

  String STATION_B_ID = "Station-B";
  String STOP_A1_ID = "A1";
  String STOP_B1_ID = "B1";
  String STOP_B2_ID = "B2";
  String STOP_C1_ID = "C1";
  String STOP_D1_ID = "D1";
}
