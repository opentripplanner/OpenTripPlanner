package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.StopIndexForRaptor;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class StopIndexForRaptorTest {

    private static Stop STOP_0 = createStop(1);
    private static Stop STOP_1 = createStop(2);
    private static Stop STOP_2 = createStop(3);
    private static Stop STOP_3 = createStop(4);
    private static Stop STOP_4 = createStop(5);

    private static List<Stop> STOPS = Arrays.asList(
            STOP_0,
            STOP_1,
            STOP_2,
            STOP_3,
            STOP_4
    );

    @Test public void listStopIndexesForEmptyTripPattern() {
        StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS);

        int[] result = stopIndex.listStopIndexesForStops(new Stop[0]);

        assertEquals(result.length, 0);
    }


    @Test public void listStopIndexesForTripPattern() {
        Stop[] input = new Stop[] {
                STOP_0,
                STOP_2,
                STOP_4
        };

        StopIndexForRaptor stopIndex = new StopIndexForRaptor(STOPS);

        int[] result = stopIndex.listStopIndexesForStops(input);

        assertEquals("[0, 2, 4]", Arrays.toString(result));
    }


    /* private methods */

    private static Stop createStop(int id) {
        Stop stop = new Stop();
        stop.setId(new FeedScopedId("agency", "ID-" + id));
        stop.setName("Stop " + id);
        return stop;
    }
}
