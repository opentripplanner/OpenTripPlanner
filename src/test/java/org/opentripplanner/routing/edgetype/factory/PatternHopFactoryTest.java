package org.opentripplanner.routing.edgetype.factory;


import java.io.IOException;

import org.junit.Test;
import org.opentripplanner.gtfs.MockGtfs;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;

public class PatternHopFactoryTest {

    @Test
    public void testBikesAllowed() throws IOException {
        MockGtfs gtfs = MockGtfs.create();
        gtfs.putAgencies(1);
        gtfs.putRoutes(1);
        gtfs.putStops(2);
        gtfs.putCalendars(1);
        gtfs.putTrips(1, "r0", "sid0", "bikes_allowed=1");
        gtfs.putStopTimes("t0", "s0,s1");
        gtfs.putLines("frequencies.txt", "trip_id,start_time,end_time,headway_secs",
                "t0,09:00:00,17:00:00,300");

        GtfsFeedId feedId = new GtfsFeedId.Builder().id("FEED").build();
        PatternHopFactory factory = new PatternHopFactory(
                GtfsLibrary.createContext(feedId, gtfs.read())
        );
        Graph graph = new Graph();
        factory.run(graph);

        for (Edge edge : graph.getEdges()) {
            if (edge instanceof TransitBoardAlight) {
                TripPattern pattern = ((TransitBoardAlight)edge).getPattern();
                // TODO assertTrue(pattern.getBikesAllowed());
            }
        }
    }

}
