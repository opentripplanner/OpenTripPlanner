package org.opentripplanner.graph_builder.module.geometry;


import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.gtfs.MockGtfs;
import org.opentripplanner.routing.graph.Graph;

import java.io.IOException;

public class GeometryAndBlockProcessorTest {

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
        Graph graph = new Graph();

        GtfsContext context = new GtfsContextBuilder(feedId, gtfs.read())
                .withIssueStoreAndDeduplicator(graph)
                .build();

        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);

        factory.run(graph);

    }

}
