package org.opentripplanner.updater.trip;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;

public interface RealtimeTestConstants {
  LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 8);
  ZoneId TIME_ZONE = ZoneId.of(TimetableRepositoryForTest.TIME_ZONE_ID);

  String TRIP_1_ID = "TestTrip1";
  String TRIP_2_ID = "TestTrip2";

  String STATION_OMEGA_ID = "Station-Omega";
  String STOP_A_ID = "A";
  String STOP_B_ID = "B";
  String STOP_C_ID = "C";
  String STOP_D_ID = "D";
}
