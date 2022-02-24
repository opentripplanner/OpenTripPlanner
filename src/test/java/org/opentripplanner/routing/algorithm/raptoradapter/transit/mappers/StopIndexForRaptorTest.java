package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.StopTransferPriority;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitTuningParameters;

public class StopIndexForRaptorTest {

    private final FeedScopedId ANY_ID = new FeedScopedId("F", "1");

    private final Stop STOP_0 = Stop.stopForTest("ID-" + 1, 0.0, 0.0);
    private final Stop STOP_1 = Stop.stopForTest("ID-" + 2, 0.0, 0.0);
    private final Stop STOP_2 = Stop.stopForTest("ID-" + 3, 0.0, 0.0);
    private final Stop STOP_3 = Stop.stopForTest("ID-" + 4, 0.0, 0.0);
    private final Stop STOP_4 = Stop.stopForTest("ID-" + 5, 0.0, 0.0);

    private final List<StopLocation> STOPS = Arrays.asList(
            STOP_0,
            STOP_1,
            STOP_2,
            STOP_3,
            STOP_4
    );

    @Test public void listStopIndexesForEmptyTripPattern() {
        StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);
        var p = new TripPattern(ANY_ID, null, new StopPattern(List.of()));

        int[] result = stopIndex.listStopIndexesForPattern(p);

        assertEquals(result.length, 0);
    }


    @Test public void listStopIndexesForTripPattern() {
        var stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);
        var tripPattern = new TripPattern(
                ANY_ID,
                null,
                new StopPattern(stopTimes(STOP_0, STOP_2, STOP_4))
        );

        int[] result = stopIndex.listStopIndexesForPattern(tripPattern);

        assertEquals("[0, 2, 4]", Arrays.toString(result));
    }

    @Test public void stopBoardAlightCosts() {
        STOP_1.setParentStation(createStation("A", StopTransferPriority.DISCOURAGED));
        STOP_2.setParentStation(createStation("B", StopTransferPriority.ALLOWED));
        STOP_3.setParentStation(createStation("C", StopTransferPriority.RECOMMENDED));
        STOP_4.setParentStation(createStation("D", StopTransferPriority.PREFERRED));

        StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);

        int[] result = stopIndex.stopBoardAlightCosts;

        // Expect cost with Raptor precision
        assertEquals("[6000, 360000, 6000, 2000, 0]", Arrays.toString(result));
    }

    Station createStation(String name, StopTransferPriority pri) {
        return new Station(ANY_ID, name, new WgsCoordinate(0, 0), null, null, null, null, pri);
    }

    private static List<StopTime> stopTimes(Stop ... stops) {
        return Arrays.stream(stops)
                .map(StopIndexForRaptorTest::stopTime)
                .collect(Collectors.toList());
    }

    private static StopTime stopTime(Stop stop) {
        var st = new StopTime();
        st.setStop(stop);
        return st;
    }
}
