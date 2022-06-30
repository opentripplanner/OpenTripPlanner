package org.opentripplanner.routing.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.GtfsTest;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.timetable.Trip;

/**
 * Check that the graph index is created, that GTFS elements can be found in the index, and that the
 * indexes are coherent with one another.
 * <p>
 * TODO: The old transit index doesn't exist anymore, and the new one needs more tests.
 */
public class RoutingServiceTest extends GtfsTest {

  @Override
  public String getFeedName() {
    return "testagency";
  }

  @Test
  public void testIdLookup() {
    /* Graph vertices */
    for (Vertex vertex : graph.getVertices()) {
      if (vertex instanceof TransitStopVertex) {
        Stop stop = ((TransitStopVertex) vertex).getStop();
        Vertex index_vertex = transitModel.getStopModel().getStopVertexForStop().get(stop);
        assertEquals(index_vertex, vertex);
      }
    }

    /* Agencies */
    String feedId = transitModel.getFeedIds().iterator().next();
    Agency agency;
    agency = transitModel.index.getAgencyForId(new FeedScopedId(feedId, "azerty"));
    assertNull(agency);
    agency = transitModel.index.getAgencyForId(new FeedScopedId(feedId, "agency"));
    assertEquals(agency.getId().toString(), feedId + ":" + "agency");
    assertEquals(agency.getName(), "Fake Agency");

    /* Stops */
    transitModel.getStopModel().getStopModelIndex().getStopForId(new FeedScopedId("X", "Y"));
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
    for (Trip trip : transitModel.index.getTripForId().values()) {
      TripPattern pattern = transitModel.index.getPatternForTrip().get(trip);
      assertTrue(pattern.scheduledTripsAsStream().anyMatch(t -> t.equals(trip)));
    }
    /* This one depends on a feed where each TripPattern appears on only one route. */
    for (Route route : transitModel.index.getAllRoutes()) {
      for (TripPattern pattern : transitModel.index.getPatternsForRoute().get(route)) {
        assertEquals(pattern.getRoute(), route);
      }
    }
    for (var stop : transitModel.getStopModel().getStopModelIndex().getAllStops()) {
      for (TripPattern pattern : transitModel.index.getPatternsForStop(stop)) {
        int stopPos = pattern.findStopPosition(stop);
        assertTrue(stopPos >= 0, "Stop position exist");
      }
    }
  }

  @Test
  public void testSpatialIndex() {
    String feedId = transitModel.getFeedIds().iterator().next();
    var stopJ = transitModel
      .getStopModel()
      .getStopModelIndex()
      .getStopForId(new FeedScopedId(feedId, "J"));
    var stopL = transitModel
      .getStopModel()
      .getStopModelIndex()
      .getStopForId(new FeedScopedId(feedId, "L"));
    var stopM = transitModel
      .getStopModel()
      .getStopModelIndex()
      .getStopForId(new FeedScopedId(feedId, "M"));
    TransitStopVertex stopvJ = transitModel
      .getStopModel()
      .getStopModelIndex()
      .getStopVertexForStop()
      .get(stopJ);
    TransitStopVertex stopvL = transitModel
      .getStopModel()
      .getStopModelIndex()
      .getStopVertexForStop()
      .get(stopL);
    TransitStopVertex stopvM = transitModel
      .getStopModel()
      .getStopModelIndex()
      .getStopVertexForStop()
      .get(stopM);
    // There are a two other stops within 100 meters of stop J.
    Envelope env = new Envelope(new Coordinate(stopJ.getLon(), stopJ.getLat()));
    env.expandBy(
      SphericalDistanceLibrary.metersToLonDegrees(100, stopJ.getLat()),
      SphericalDistanceLibrary.metersToDegrees(100)
    );
    List<TransitStopVertex> stops = transitModel
      .getStopModel()
      .getStopModelIndex()
      .getStopSpatialIndex()
      .query(env);
    assertTrue(stops.contains(stopvJ));
    assertTrue(stops.contains(stopvL));
    assertTrue(stops.contains(stopvM));
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
