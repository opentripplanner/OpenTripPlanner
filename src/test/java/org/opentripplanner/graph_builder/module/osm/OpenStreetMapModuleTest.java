package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.specifier.BestMatchSpecifier;
import org.opentripplanner.graph_builder.module.osm.specifier.OsmSpecifier;
import org.opentripplanner.openstreetmap.OpenStreetMapProvider;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.basic.LocalizedString;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class OpenStreetMapModuleTest {

  @Test
  public void testGraphBuilder() {
    var deduplicator = new Deduplicator();
    var gg = new Graph(deduplicator);

    File file = new File(
      URLDecoder.decode(getClass().getResource("map.osm.pbf").getFile(), StandardCharsets.UTF_8)
    );

    OpenStreetMapProvider provider = new OpenStreetMapProvider(file, true);

    OpenStreetMapModule osmModule = new OpenStreetMapModule(
      List.of(provider),
      Set.of(),
      gg,
      DataImportIssueStore.NOOP,
      true
    );

    osmModule.buildGraph();

    // Kamiennogorska at south end of segment
    Vertex v1 = gg.getVertex("osm:node:280592578");

    // Kamiennogorska at Mariana Smoluchowskiego
    Vertex v2 = gg.getVertex("osm:node:288969929");

    // Mariana Smoluchowskiego, north end
    Vertex v3 = gg.getVertex("osm:node:280107802");

    // Mariana Smoluchowskiego, south end (of segment connected to v2)
    Vertex v4 = gg.getVertex("osm:node:288970952");

    assertNotNull(v1);
    assertNotNull(v2);
    assertNotNull(v3);
    assertNotNull(v4);

    Edge e1 = null, e2 = null, e3 = null;
    for (Edge e : v2.getOutgoing()) {
      if (e.getToVertex() == v1) {
        e1 = e;
      } else if (e.getToVertex() == v3) {
        e2 = e;
      } else if (e.getToVertex() == v4) {
        e3 = e;
      }
    }

    assertNotNull(e1);
    assertNotNull(e2);
    assertNotNull(e3);

    assertTrue(
      e1.getDefaultName().contains("Kamiennog\u00F3rska"),
      "name of e1 must be like \"Kamiennog\u00F3rska\"; was " + e1.getDefaultName()
    );
    assertTrue(
      e2.getDefaultName().contains("Mariana Smoluchowskiego"),
      "name of e2 must be like \"Mariana Smoluchowskiego\"; was " + e2.getDefaultName()
    );
  }

  /**
   * Detailed testing of OSM graph building using a very small chunk of NYC (SOHO-ish).
   */
  @Test
  public void testBuildGraphDetailed() {
    var deduplicator = new Deduplicator();
    var gg = new Graph(deduplicator);

    File file = new File(
      URLDecoder.decode(
        getClass().getResource("NYC_small.osm.pbf").getFile(),
        StandardCharsets.UTF_8
      )
    );
    OpenStreetMapProvider provider = new OpenStreetMapProvider(file, true);
    OpenStreetMapModule osmModule = new OpenStreetMapModule(
      List.of(provider),
      Set.of(),
      gg,
      DataImportIssueStore.NOOP,
      true
    );

    osmModule.buildGraph();

    // These vertices are labeled in the OSM file as having traffic lights.
    IntersectionVertex iv1 = (IntersectionVertex) gg.getVertex("osm:node:1919595918");
    IntersectionVertex iv2 = (IntersectionVertex) gg.getVertex("osm:node:42442273");
    IntersectionVertex iv3 = (IntersectionVertex) gg.getVertex("osm:node:1919595927");
    IntersectionVertex iv4 = (IntersectionVertex) gg.getVertex("osm:node:42452026");
    assertTrue(iv1.trafficLight);
    assertTrue(iv2.trafficLight);
    assertTrue(iv3.trafficLight);
    assertTrue(iv4.trafficLight);

    // These are not.
    IntersectionVertex iv5 = (IntersectionVertex) gg.getVertex("osm:node:42435485");
    IntersectionVertex iv6 = (IntersectionVertex) gg.getVertex("osm:node:42439335");
    IntersectionVertex iv7 = (IntersectionVertex) gg.getVertex("osm:node:42436761");
    IntersectionVertex iv8 = (IntersectionVertex) gg.getVertex("osm:node:42442291");
    assertFalse(iv5.trafficLight);
    assertFalse(iv6.trafficLight);
    assertFalse(iv7.trafficLight);
    assertFalse(iv8.trafficLight);

    Set<VertexPair> edgeEndpoints = new HashSet<>();
    for (StreetEdge se : gg.getStreetEdges()) {
      var endpoints = new VertexPair(se.getFromVertex(), se.getToVertex());
      // Check that we don't get any duplicate edges on this small graph.
      if (edgeEndpoints.contains(endpoints)) {
        fail();
      }
      edgeEndpoints.add(endpoints);
    }
  }

  @Test
  public void testBuildAreaWithoutVisibility() {
    testBuildingAreas(true);
  }

  @Test
  public void testBuildAreaWithVisibility() {
    testBuildingAreas(false);
  }

  @Test
  public void testWayDataSet() {
    OSMWithTags way = new OSMWay();
    way.addTag("highway", "footway");
    way.addTag("cycleway", "lane");
    way.addTag("access", "no");
    way.addTag("surface", "gravel");

    WayPropertySet wayPropertySet = new WayPropertySet();

    // where there are no way specifiers, the default is used
    WayProperties wayData = wayPropertySet.getDataForWay(way);
    assertEquals(wayData.getPermission(), ALL);
    assertEquals(wayData.getWalkSafetyFeatures().forward(), 1.0);
    assertEquals(wayData.getWalkSafetyFeatures().back(), 1.0);
    assertEquals(wayData.getBicycleSafetyFeatures().forward(), 1.0);
    assertEquals(wayData.getBicycleSafetyFeatures().back(), 1.0);

    // add two equal matches: lane only...
    OsmSpecifier lane_only = new BestMatchSpecifier("cycleway=lane");

    WayProperties lane_is_safer = withModes(ALL).bicycleSafety(1.5).walkSafety(1.0).build();

    wayPropertySet.addProperties(lane_only, lane_is_safer);

    // and footway only
    OsmSpecifier footway_only = new BestMatchSpecifier("highway=footway");

    WayProperties footways_allow_peds = new WayPropertiesBuilder(PEDESTRIAN).build();

    wayPropertySet.addProperties(footway_only, footways_allow_peds);

    WayProperties dataForWay = wayPropertySet.getDataForWay(way);
    // the first one is found
    assertEquals(dataForWay, lane_is_safer);

    // add a better match
    OsmSpecifier lane_and_footway = new BestMatchSpecifier("cycleway=lane;highway=footway");

    WayProperties safer_and_peds = new WayPropertiesBuilder(PEDESTRIAN)
      .bicycleSafety(0.75)
      .walkSafety(1.0)
      .build();

    wayPropertySet.addProperties(lane_and_footway, safer_and_peds);
    dataForWay = wayPropertySet.getDataForWay(way);
    assertEquals(dataForWay, safer_and_peds);

    // add a mixin
    BestMatchSpecifier gravel = new BestMatchSpecifier("surface=gravel");
    WayProperties gravel_is_dangerous = new WayPropertiesBuilder(ALL).bicycleSafety(2).build();
    wayPropertySet.addProperties(gravel, gravel_is_dangerous, true);

    dataForWay = wayPropertySet.getDataForWay(way);
    assertEquals(dataForWay.getBicycleSafetyFeatures().forward(), 1.5);

    // test a left-right distinction
    way = new OSMWay();
    way.addTag("highway", "footway");
    way.addTag("cycleway", "lane");
    way.addTag("cycleway:right", "track");

    OsmSpecifier track_only = new BestMatchSpecifier("highway=footway;cycleway=track");
    WayProperties track_is_safest = new WayPropertiesBuilder(ALL)
      .bicycleSafety(0.25)
      .walkSafety(1.0)
      .build();

    wayPropertySet.addProperties(track_only, track_is_safest);
    dataForWay = wayPropertySet.getDataForWay(way);
    assertEquals(0.25, dataForWay.getBicycleSafetyFeatures().forward()); // right (with traffic) comes
    // from track
    assertEquals(0.75, dataForWay.getBicycleSafetyFeatures().back()); // left comes from lane

    way = new OSMWay();
    way.addTag("highway", "footway");
    way.addTag("footway", "sidewalk");
    way.addTag("RLIS:reviewed", "no");
    WayPropertySet propset = new WayPropertySet();
    CreativeNamer namer = new CreativeNamer("platform");
    propset.addCreativeNamer(
      new BestMatchSpecifier("railway=platform;highway=footway;footway=sidewalk"),
      namer
    );
    namer = new CreativeNamer("sidewalk");
    propset.addCreativeNamer(new BestMatchSpecifier("highway=footway;footway=sidewalk"), namer);
    assertEquals("sidewalk", propset.getCreativeNameForWay(way).toString());
  }

  @Test
  public void testCreativeNaming() {
    OSMWithTags way = new OSMWay();
    way.addTag("highway", "footway");
    way.addTag("cycleway", "lane");
    way.addTag("access", "no");

    CreativeNamer namer = new CreativeNamer();
    namer.setCreativeNamePattern(
      "Highway with cycleway {cycleway} and access {access} and morx {morx}"
    );
    assertEquals(
      "Highway with cycleway lane and access no and morx ",
      namer.generateCreativeName(way).toString()
    );
  }

  @Test
  public void testLocalizedString() {
    LocalizedString localizedString = new LocalizedString(
      "corner",
      new NonLocalizedString("first"),
      new NonLocalizedString("second")
    );

    assertEquals("Kreuzung first mit second", localizedString.toString(new Locale("de")));
  }

  @Test
  void addParkingLotsToService() {
    var graph = new Graph();
    var providers = Stream
      .of("B+R.osm.pbf", "P+R.osm.pbf")
      .map(f -> new File(getClass().getResource(f).getFile()))
      .map(f -> new OpenStreetMapProvider(f, false))
      .toList();
    var module = new OpenStreetMapModule(
      providers,
      Set.of(),
      graph,
      DataImportIssueStore.NOOP,
      false
    );
    module.staticParkAndRide = true;
    module.staticBikeParkAndRide = true;
    module.buildGraph();

    var service = graph.getVehicleParkingService();
    assertEquals(8, service.getVehicleParkings().count());
    assertEquals(4, service.getBikeParks().count());
    assertEquals(4, service.getCarParks().count());
  }

  /**
   * This reads test file with area and tests if it can be routed if visibility is used and if it
   * isn't
   * <p>
   * Routing needs to be successful in both options since without visibility calculation area rings
   * are used.
   *
   * @param skipVisibility if true visibility calculations are skipped
   */
  private void testBuildingAreas(boolean skipVisibility) {
    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);

    File file = new File(
      URLDecoder.decode(
        getClass().getResource("usf_area.osm.pbf").getFile(),
        StandardCharsets.UTF_8
      )
    );
    OpenStreetMapProvider provider = new OpenStreetMapProvider(file, false);

    OpenStreetMapModule loader = new OpenStreetMapModule(
      List.of(provider),
      Set.of(),
      graph,
      DataImportIssueStore.NOOP,
      !skipVisibility
    );

    loader.buildGraph();

    RouteRequest request = new RouteRequest();

    //This are vertices that can be connected only over edges on area (with correct permissions)
    //It tests if it is possible to route over area without visibility calculations
    Vertex bottomV = graph.getVertex("osm:node:580290955");
    Vertex topV = graph.getVertex("osm:node:559271124");

    GraphPathFinder graphPathFinder = new GraphPathFinder(null, Duration.ofSeconds(3));
    List<GraphPath<State, Edge, Vertex>> pathList = graphPathFinder.graphPathFinderEntryPoint(
      request,
      Set.of(bottomV),
      Set.of(topV)
    );

    assertNotNull(pathList);
    assertFalse(pathList.isEmpty());
    for (GraphPath<State, Edge, Vertex> path : pathList) {
      assertFalse(path.states.isEmpty());
    }
  }

  private record VertexPair(Vertex v0, Vertex v1) {}
}
