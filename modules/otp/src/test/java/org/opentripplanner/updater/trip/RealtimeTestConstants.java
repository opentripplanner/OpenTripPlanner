package org.opentripplanner.updater.trip;

import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.time.LocalDate;
import java.time.ZoneId;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
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
  Operator OPERATOR1 = Operator.of(id(OPERATOR_1_ID)).withName(OPERATOR_1_ID).build();
  String ROUTE_1_ID = "TestRoute1";

  TransitModelForTest TEST_MODEL = TransitModelForTest.of();
  ZoneId TIME_ZONE = ZoneId.of(TransitModelForTest.TIME_ZONE_ID);
  Station STATION_A = TEST_MODEL.station("A").build();
  Station STATION_B = TEST_MODEL.station("B").build();
  Station STATION_C = TEST_MODEL.station("C").build();
  Station STATION_D = TEST_MODEL.station("D").build();
  RegularStop STOP_A1 = TEST_MODEL.stop(STOP_A1_ID).withParentStation(STATION_A).build();
  RegularStop STOP_B1 = TEST_MODEL.stop(STOP_B1_ID).withParentStation(STATION_B).build();
  RegularStop STOP_B2 = TEST_MODEL.stop("B2").withParentStation(STATION_B).build();
  RegularStop STOP_C1 = TEST_MODEL.stop(STOP_C1_ID).withParentStation(STATION_C).build();
  RegularStop STOP_D1 = TEST_MODEL.stop("D1").withParentStation(STATION_D).build();
  StopModel STOP_MODEL = TEST_MODEL
    .stopModelBuilder()
    .withRegularStop(STOP_A1)
    .withRegularStop(STOP_B1)
    .withRegularStop(STOP_B2)
    .withRegularStop(STOP_C1)
    .withRegularStop(STOP_D1)
    .build();

  Route ROUTE_1 = TransitModelForTest.route(ROUTE_1_ID).build();
}
