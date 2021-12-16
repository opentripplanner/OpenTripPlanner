package org.opentripplanner.routing.algorithm.filterchain.groupids;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.plan.PlanTestConstants;

class GroupByAllSameStationsTest implements PlanTestConstants {

    @Test
    public void testMatchingItineraries() {
        Station STATION_1 = createStation("1");
        Station STATION_2 = createStation("2");
        Station STATION_3 = createStation("3");
        Station STATION_4 = createStation("4");


        ((Stop) A.stop).setParentStation(STATION_1);
        ((Stop) B.stop).setParentStation(STATION_2);
        ((Stop) C.stop).setParentStation(STATION_2);
        ((Stop) D.stop).setParentStation(STATION_3);
        ((Stop) E.stop).setParentStation(STATION_3);
        ((Stop) F.stop).setParentStation(STATION_4);

        GroupByAllSameStations first = new GroupByAllSameStations(newItinerary(A)
                .rail(20, T11_05, T11_15, B)
                .walk(D5m, C)
                .bus(30, T11_30, T11_50, D)
                .build());

        GroupByAllSameStations second = new GroupByAllSameStations(newItinerary(A)
                .rail(20, T11_05, T11_15, B)
                .walk(D5m, C)
                .bus(30, T11_30, T11_50, E)
                .build());

        GroupByAllSameStations third = new GroupByAllSameStations(newItinerary(A)
                .rail(20, T11_05, T11_15, B)
                .walk(D5m, C)
                .bus(30, T11_30, T11_50, F)
                .build());

        GroupByAllSameStations fourth = new GroupByAllSameStations(newItinerary(A)
                .rail(20, T11_05, T11_15, B)
                .walk(D5m, C)
                .bus(30, T11_30, T11_50, G)
                .build());

        GroupByAllSameStations withoutTransferWalk = new GroupByAllSameStations(newItinerary(A)
                .rail(20, T11_05, T11_15, B)
                .bus(30, T11_30, T11_50, E)
                .build());

        GroupByAllSameStations withAccessEgressWalk = new GroupByAllSameStations(newItinerary(H, T11_01)
                .walk(D2m, A)
                .rail(20, T11_05, T11_15, B)
                .bus(30, T11_30, T11_50, E)
                .walk(D5m, F)
                .build());

        assertTrue(first.match(second));
        assertFalse(first.match(third));
        assertFalse(first.match(fourth));
        assertTrue(first.match(withoutTransferWalk));
        assertTrue(first.match(withAccessEgressWalk));

    }


    Station createStation(String name) {
        return new Station(new FeedScopedId(FEED_ID, name), name, new WgsCoordinate(0, 0), null, null, null, null, null);
    }

}