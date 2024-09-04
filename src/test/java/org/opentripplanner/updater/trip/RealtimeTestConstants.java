package org.opentripplanner.updater.trip;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.StopModel;

public interface RealtimeTestConstants {
  LocalDate SERVICE_DATE = LocalDate.of(2024, 5, 8);
  FeedScopedId SERVICE_ID = TransitModelForTest.id("CAL_1");
  String STOP_A1_ID = "A1";
  String STOP_B1_ID = "B1";
  String STOP_C1_ID = "C1";
  String TRIP_1_ID = "TestTrip1";
  String TRIP_2_ID = "TestTrip2";
  String OPERATOR_1_ID = "TestOperator1";
  String ROUTE_1_ID = "TestRoute1";

  TransitModelForTest testModel = TransitModelForTest.of();
  ZoneId TIME_ZONE = ZoneId.of(TransitModelForTest.TIME_ZONE_ID);
  Station STATION_A = testModel.station("A").build();
  Station stationB = testModel.station("B").build();
  Station stationC = testModel.station("C").build();
  Station stationD = testModel.station("D").build();
  RegularStop STOP_A1 = testModel.stop(STOP_A1_ID).withParentStation(STATION_A).build();
  RegularStop STOP_B1 = testModel.stop(STOP_B1_ID).withParentStation(stationB).build();
  RegularStop STOP_B2 = testModel.stop("B2").withParentStation(stationB).build();
  RegularStop STOP_C1 = testModel.stop(STOP_C1_ID).withParentStation(stationC).build();
  RegularStop STOP_D1 = testModel.stop("D1").withParentStation(stationD).build();
  StopModel STOP_MODEL = testModel
    .stopModelBuilder()
    .withRegularStop(STOP_A1)
    .withRegularStop(STOP_B1)
    .withRegularStop(STOP_B2)
    .withRegularStop(STOP_C1)
    .withRegularStop(STOP_D1)
    .build();

  Route route1 = TransitModelForTest.route(ROUTE_1_ID).build();
}
