package org.opentripplanner.routing.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.Feed;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

/**
 * Check that the graph index is created, that GTFS elements can be found in the index, and that the
 * indexes are coherent with one another.
 * <p>
 * TODO: The old transit index doesn't exist anymore, and the new one needs more tests.
 */
public class DefaultRoutingServiceTest extends GtfsTest {

  private TransitService transitService;

  @BeforeEach
  @Override
  public void setUp() throws Exception {
    super.setUp();
    transitService = new DefaultTransitService(timetableRepository);
  }

  @Override
  public String getFeedName() {
    return "gtfs/simple";
  }

  @Test
  public void testIdLookup() {
    /* Graph vertices */
    for (Vertex vertex : graph.getVertices()) {
      if (vertex instanceof TransitStopVertex) {
        RegularStop stop = ((TransitStopVertex) vertex).getStop();
        Vertex index_vertex = graph.getStopVertexForStopId(stop.getId());
        assertEquals(index_vertex, vertex);
      }
    }

    /* Agencies */
    Feed feed = transitService.listFeeds().iterator().next();
    Agency agency;
    agency = transitService.getAgency(feed.scopedId("azerty"));
    assertNull(agency);
    agency = transitService.getAgency(feed.scopedId("agency"));
    assertEquals(feed + ":" + "agency", agency.getId().toString());
    assertEquals("Fake Agency", agency.getName());

    /* Stops */
    transitService.getRegularStop(new FeedScopedId("X", "Y"));
    /* Trips */
    //        graph.index.tripForId;
    //        graph.index.routeForId;
    //        graph.index.serviceForId;
    //        graph.index.patternForId;
  }

  /**
   * Check that bidirectional relationships between TripPatterns and Trips, Routes, and Stops are
   * coherent.
   */
  @Test
  public void testPatternsCoherent() {
    for (Trip trip : transitService.listTrips()) {
      TripPattern pattern = transitService.findPattern(trip);
      assertTrue(pattern.scheduledTripsAsStream().anyMatch(t -> t.equals(trip)));
    }
    /* This one depends on a feed where each TripPattern appears on only one route. */
    for (Route route : transitService.listRoutes()) {
      for (TripPattern pattern : transitService.findPatterns(route)) {
        assertEquals(pattern.getRoute(), route);
      }
    }
    for (var stop : transitService.listStopLocations()) {
      for (TripPattern pattern : transitService.findPatterns(stop)) {
        int stopPos = pattern.findStopPosition(stop);
        assertTrue(stopPos >= 0, "Stop position exist");
      }
    }
  }

  @Test
  public void testSpatialIndex() {
    Feed feed = transitService.listFeeds().iterator().next();
    FeedScopedId idJ = feed.scopedId("J");
    var stopJ = transitService.getRegularStop(idJ);
    FeedScopedId idL = feed.scopedId("L");
    var stopL = transitService.getRegularStop(idL);
    FeedScopedId idM = feed.scopedId("M");
    var stopM = transitService.getRegularStop(idM);
    TransitStopVertex stopvJ = graph.getStopVertexForStopId(idJ);
    TransitStopVertex stopvL = graph.getStopVertexForStopId(idL);
    TransitStopVertex stopvM = graph.getStopVertexForStopId(idM);
    // There are a two other stops within 100 meters of stop J.
    Envelope env = new Envelope(new Coordinate(stopJ.getLon(), stopJ.getLat()));
    env.expandBy(
      SphericalDistanceLibrary.metersToLonDegrees(100, stopJ.getLat()),
      SphericalDistanceLibrary.metersToDegrees(100)
    );
    Collection<RegularStop> stops = transitService.findRegularStopsByBoundingBox(env);
    assertTrue(stops.contains(stopJ));
    assertTrue(stops.contains(stopL));
    assertTrue(stops.contains(stopM));
    assertTrue(stops.size() >= 3); // Query can overselect
  }

  @Test
  public void testParentStations() {
    // graph.index.stopsForParentStation;
  }

  @Test
  public void testLucene() {
    // graph.index.luceneIndex
  }
}
