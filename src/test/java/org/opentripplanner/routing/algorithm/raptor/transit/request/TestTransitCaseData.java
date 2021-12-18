package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.time.LocalDate;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;

public interface TestTransitCaseData {
    Station STATION_A = Station.stationForTest("A", 60.0, 11.1);
    Station STATION_B = Station.stationForTest("B", 61.0, 11.5);

    Stop STOP_A = Stop.stopForTest("A", 60.0, 11.0, STATION_A);
    Stop STOP_B = Stop.stopForTest("B", 60.0, 11.2, STATION_B);
    Stop STOP_C = Stop.stopForTest("C", 61.0, 11.4);
    Stop STOP_D = Stop.stopForTest("D", 61.0, 11.6);

    // Random order stop indexes - should be different from stopPos in pattern to
    // make sure code-under-test do not mix stopIndex and stopPosition
    Stop[] RAPTOR_STOP_INDEX = { STOP_D, STOP_A, STOP_C, STOP_B };

    LocalDate DATE = LocalDate.of(2021, 12, 24);

    int OFFSET = 0;


    static FeedScopedId id(String id) {
        return new FeedScopedId("F", id);
    }

    default int stopIndex(StopLocation stop) {
        for (int i=0;i< RAPTOR_STOP_INDEX.length;++i) {
            if(stop == RAPTOR_STOP_INDEX[i]) { return i; }
        }
        throw new IllegalArgumentException();
    }
}
