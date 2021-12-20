package org.opentripplanner.routing.algorithm.raptor.transit.request;

import java.time.LocalDate;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;

public interface TestTransitCaseData {
    String FEED_ID = "F";

    Stop STOP_A = Stop.stopForTest("A", 60.0, 11.0);
    Stop STOP_B = Stop.stopForTest("B", 60.1, 11.1);
    Stop STOP_C = Stop.stopForTest("C", 60.2, 11.2);
    Stop STOP_D = Stop.stopForTest("D", 60.3, 11.3);

    // Random order stop indexes - should be different from stopPos in pattern to
    // make sure code-under-test do not mix stopIndex and stopPosition
    Stop[] RAPTOR_STOP_INDEX = { STOP_D, STOP_A, STOP_C, STOP_B };

    LocalDate DATE = LocalDate.of(2021, 12, 24);

    int OFFSET = 0;


    default FeedScopedId id(String id) {
        return new FeedScopedId(FEED_ID, id);
    }

    default int stopIndex(StopLocation stop) {
        for (int i=0;i< RAPTOR_STOP_INDEX.length;++i) {
            if(stop == RAPTOR_STOP_INDEX[i]) { return i; }
        }
        throw new IllegalArgumentException();
    }
}
