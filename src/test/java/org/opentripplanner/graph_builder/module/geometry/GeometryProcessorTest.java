package org.opentripplanner.graph_builder.module.geometry;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.gtfs.MockGtfs;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

public class GeometryProcessorTest {

  @Test
  public void testBikesAllowed() throws IOException {
    MockGtfs gtfs = MockGtfs.create();
    gtfs.putAgencies(1);
    gtfs.putRoutes(1);
    gtfs.putStops(2);
    gtfs.putCalendars(1);
    gtfs.putTrips(1, "r0", "sid0", "bikes_allowed=1");
    gtfs.putStopTimes("t0", "s0,s1");
    gtfs.putLines(
      "frequencies.txt",
      "trip_id,start_time,end_time,headway_secs",
      "t0,09:00:00,17:00:00,300"
    );

    GtfsFeedId feedId = new GtfsFeedId.Builder().id("FEED").build();
    var deduplicator = new Deduplicator();
    var stopModel = new StopModel();
    var graph = new Graph(stopModel, deduplicator);
    var transitModel = new TransitModel(stopModel, deduplicator);

    GtfsContext context = new GtfsContextBuilder(feedId, gtfs.read())
      .withIssueStoreAndDeduplicator(graph)
      .build();

    GeometryProcessor processor = new GeometryProcessor(context);

    processor.run(transitModel);
  }
}
