package org.opentripplanner.graph_builder.module.geometry;

import static graphql.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.module.GtfsFeedId;
import org.opentripplanner.graph_builder.module.GtfsModule;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.gtfs.MockGtfs;
import org.opentripplanner.model.calendar.ServiceDateInterval;
import org.opentripplanner.routing.graph.Graph;

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
    gtfs.putLines(
      "frequencies.txt",
      "trip_id,start_time,end_time,headway_secs",
      "t0,09:00:00,17:00:00,300"
    );

    GtfsFeedId feedId = new GtfsFeedId.Builder().id("FEED").build();
    Graph graph = new Graph();

    GtfsContext context = new GtfsContextBuilder(feedId, gtfs.read())
      .withIssueStoreAndDeduplicator(graph)
      .build();

    GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);

    factory.run(graph);
  }

  @Test
  public void addShapesForFrequencyTrips() {
    var graph = new Graph();

    var bundle = new GtfsBundle(new File(ConstantsForTests.FAKE_GTFS));
    bundle.setFeedId(new GtfsFeedId.Builder().id("1").build());
    var module = new GtfsModule(List.of(bundle), ServiceDateInterval.unbounded(), null, false);

    module.buildGraph(graph, new HashMap<>());

    var frequencyTripPattern = graph
      .getTripPatterns()
      .stream()
      .filter(p -> !p.getScheduledTimetable().getFrequencyEntries().isEmpty())
      .toList();

    assertEquals(1, frequencyTripPattern.size());

    var id = frequencyTripPattern.get(0);

    var pattern = graph.getTripPatternForId(id.getId());
    assertNotNull(pattern.getGeometry());
    assertNotNull(pattern.getHopGeometry(0));
  }
}
