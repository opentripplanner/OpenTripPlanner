package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransferPriority;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitTuningParameters;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StopIndexForRaptorTest {

    private final Stop STOP_0 = Stop.stopForTest("ID-" + 1, 0.0, 0.0);
    private final Stop STOP_1 = Stop.stopForTest("ID-" + 2, 0.0, 0.0);
    private final Stop STOP_2 = Stop.stopForTest("ID-" + 3, 0.0, 0.0);
    private final Stop STOP_3 = Stop.stopForTest("ID-" + 4, 0.0, 0.0);
    private final Stop STOP_4 = Stop.stopForTest("ID-" + 5, 0.0, 0.0);

    private final List<Stop> STOPS = Arrays.asList(
            STOP_0,
            STOP_1,
            STOP_2,
            STOP_3,
            STOP_4
    );

    @Test public void listStopIndexesForEmptyTripPattern() {
        StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);

        int[] result = stopIndex.listStopIndexesForStops(new Stop[0]);

        assertEquals(result.length, 0);
    }


    @Test public void listStopIndexesForTripPattern() {
        Stop[] input = new Stop[] {
                STOP_0,
                STOP_2,
                STOP_4
        };

        StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);

        int[] result = stopIndex.listStopIndexesForStops(input);

        assertEquals("[0, 2, 4]", Arrays.toString(result));
    }

    @Test public void stopBoardAlightCosts() {
        STOP_1.setParentStation(createStation("A", TransferPriority.DISCOURAGED));
        STOP_2.setParentStation(createStation("B", TransferPriority.ALLOWED));
        STOP_3.setParentStation(createStation("C", TransferPriority.RECOMMENDED));
        STOP_4.setParentStation(createStation("D", TransferPriority.PREFERRED));

        StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS, TransitTuningParameters.FOR_TEST);

        int[] result = stopIndex.stopBoardAlightCosts;

        // Expect cost with Raptor precision
        assertEquals("[6000, 360000, 6000, 2000, 0]", Arrays.toString(result));
    }

    Station createStation(String name, TransferPriority pri) {
        return new Station(new FeedScopedId("F", name), name, null, null, null, null, null, pri);
    }
}
