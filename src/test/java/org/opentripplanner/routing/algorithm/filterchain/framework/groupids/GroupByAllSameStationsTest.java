package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.PlanTestConstants;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;

class GroupByAllSameStationsTest implements PlanTestConstants {

  private final TransitModelForTest testModel = TransitModelForTest.of();

  Station STATION_1 = testModel.station("1").build();
  Station STATION_2 = testModel.station("2").build();
  Station STATION_3 = testModel.station("3").build();
  Station STATION_4 = testModel.station("4").build();

  Place P_A = place("A", 5.0, 8.0, STATION_1);
  Place P_B = place("B", 6.0, 8.5, STATION_2);
  Place P_C = place("C", 7.0, 9.0, STATION_2);
  Place P_D = place("D", 8.0, 9.5, STATION_3);
  Place P_E = place("E", 9.0, 10.0, STATION_3);
  Place P_F = place("F", 9.0, 10.5, STATION_4);
  Place P_G = place("G", 9.5, 11.0, null);
  Place P_H = place("H", 10.0, 11.5, null);

  @Test
  public void testMatchingItineraries() {
    GroupByAllSameStations first = new GroupByAllSameStations(
      newItinerary(P_A)
        .rail(20, T11_05, T11_15, P_B)
        .walk(D5m, P_C)
        .bus(30, T11_30, T11_50, P_D)
        .build()
    );

    GroupByAllSameStations second = new GroupByAllSameStations(
      newItinerary(P_A)
        .rail(20, T11_05, T11_15, P_B)
        .walk(D5m, P_C)
        .bus(30, T11_30, T11_50, P_E)
        .build()
    );

    GroupByAllSameStations third = new GroupByAllSameStations(
      newItinerary(P_A)
        .rail(20, T11_05, T11_15, P_B)
        .walk(D5m, P_C)
        .bus(30, T11_30, T11_50, P_F)
        .build()
    );

    GroupByAllSameStations fourth = new GroupByAllSameStations(
      newItinerary(P_A)
        .rail(20, T11_05, T11_15, P_B)
        .walk(D5m, P_C)
        .bus(30, T11_30, T11_50, P_G)
        .build()
    );

    GroupByAllSameStations withoutTransferWalk = new GroupByAllSameStations(
      newItinerary(P_A).rail(20, T11_05, T11_15, P_B).bus(30, T11_30, T11_50, P_E).build()
    );

    GroupByAllSameStations withAccessEgressWalk = new GroupByAllSameStations(
      newItinerary(P_H, T11_01)
        .walk(D2m, P_A)
        .rail(20, T11_05, T11_15, P_B)
        .bus(30, T11_30, T11_50, P_E)
        .walk(D5m, P_F)
        .build()
    );

    assertTrue(first.match(second));
    assertFalse(first.match(third));
    assertFalse(first.match(fourth));
    assertTrue(first.match(withoutTransferWalk));
    assertTrue(first.match(withAccessEgressWalk));
  }

  Place place(String name, double lat, double lon, Station parent) {
    RegularStopBuilder stop = testModel.stop(name).withCoordinate(lat, lon);
    if (parent != null) {
      stop.withParentStation(parent);
    }
    return Place.forStop(stop.build());
  }
}
