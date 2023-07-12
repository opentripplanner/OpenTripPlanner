package org.opentripplanner.ext.flex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.geometry.Coordinates.BERLIN;
import static org.opentripplanner._support.geometry.Coordinates.HAMBURG;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;

class FlexLocationsToStreetEdgesMapperTest {

  private static final AreaStop BERLIN_AREA_STOP = TransitModelForTest.areaStopForTest(
    "berlin",
    Polygons.BERLIN
  );
  public static final StopModel STOP_MODEL = StopModel
    .of()
    .withAreaStop(FlexLocationsToStreetEdgesMapperTest.BERLIN_AREA_STOP)
    .build();

  public static final TransitModel TRANSIT_MODEL = new TransitModel(STOP_MODEL, new Deduplicator());

  @Test
  void addAreaStopToVertex() {
    var graph = new Graph();

    var berlinCenterA = StreetModelForTest.intersectionVertex(BERLIN);
    var berlinCenterB = StreetModelForTest.intersectionVertex(BERLIN);

    var edge = StreetModelForTest.streetEdge(berlinCenterA, berlinCenterB);
    edge.setPermission(StreetTraversalPermission.ALL);
    berlinCenterA.addOutgoing(edge);

    graph.addVertex(berlinCenterA);
    assertTrue(berlinCenterA.areaStops().isEmpty());

    var mapper = new FlexLocationsToStreetEdgesMapper(graph, TRANSIT_MODEL);

    mapper.buildGraph();

    assertEquals(Set.of(BERLIN_AREA_STOP), berlinCenterA.areaStops());
  }

  @Test
  void dontAddOutsideOfGeometry() {
    var graph = new Graph();

    var hamburgA = StreetModelForTest.intersectionVertex(HAMBURG);
    var hamburgB = StreetModelForTest.intersectionVertex(HAMBURG);

    var edge = StreetModelForTest.streetEdge(hamburgA, hamburgB);
    edge.setPermission(StreetTraversalPermission.ALL);
    hamburgA.addOutgoing(edge);

    graph.addVertex(hamburgA);
    assertTrue(hamburgA.areaStops().isEmpty());

    var mapper = new FlexLocationsToStreetEdgesMapper(graph, TRANSIT_MODEL);

    mapper.buildGraph();

    assertEquals(Set.of(), hamburgA.areaStops());
  }

  @Test
  void dontAddIfNotEligibleForCarPickup() {
    var graph = new Graph();

    var berlinCenterA = StreetModelForTest.intersectionVertex(BERLIN);
    var berlinCenterB = StreetModelForTest.intersectionVertex(BERLIN);

    var edge = StreetModelForTest.streetEdge(berlinCenterA, berlinCenterB);
    edge.setPermission(StreetTraversalPermission.PEDESTRIAN);
    berlinCenterA.addOutgoing(edge);

    graph.addVertex(berlinCenterA);
    assertTrue(berlinCenterA.areaStops().isEmpty());

    var mapper = new FlexLocationsToStreetEdgesMapper(graph, TRANSIT_MODEL);

    mapper.buildGraph();

    assertEquals(Set.of(), berlinCenterA.areaStops());
  }
}
