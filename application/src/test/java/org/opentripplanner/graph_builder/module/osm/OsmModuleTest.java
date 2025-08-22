package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.graph_builder.module.osm.VertexGeneratorTest.getBarrierLevelIssues;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;
import static org.opentripplanner.transit.model.basic.Accessibility.NOT_POSSIBLE;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.moduletests._support.TestOsmProvider;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.CreativeNamer;
import org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.osm.wayproperty.WayPropertiesBuilder;
import org.opentripplanner.osm.wayproperty.WayPropertiesPair;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.BestMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.OsmVertexOnWay;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsmModuleTest {

  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(OsmModuleTest.class);
  private static final Logger log = LoggerFactory.getLogger(OsmModuleTest.class);

  @Test
  public void testGraphBuilder() {
    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);

    File file = RESOURCE_LOADER.file("map.osm.pbf");

    DefaultOsmProvider provider = new DefaultOsmProvider(file, true);

    OsmModule osmModule = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    )
      .withAreaVisibility(true)
      .build();

    osmModule.buildGraph();

    // Kamiennogorska at south end of segment
    Vertex v1 = graph.getVertex(VertexLabel.osm(280592578));

    // Kamiennogorska at Mariana Smoluchowskiego
    Vertex v2 = graph.getVertex(VertexLabel.osm(288969929));

    // Mariana Smoluchowskiego, north end
    Vertex v3 = graph.getVertex(VertexLabel.osm(280107802));

    // Mariana Smoluchowskiego, south end (of segment connected to v2)
    Vertex v4 = graph.getVertex(VertexLabel.osm(288970952));

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

    File file = RESOURCE_LOADER.file("NYC_small.osm.pbf");
    var provider = new DefaultOsmProvider(file, true);
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var vehicleParkingRepository = new DefaultVehicleParkingRepository();
    var osmModule = OsmModule.of(provider, gg, osmInfoRepository, vehicleParkingRepository)
      .withAreaVisibility(true)
      .build();

    osmModule.buildGraph();

    // These vertices are labeled in the OSM file as having traffic lights.
    IntersectionVertex iv1 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(1919595918));
    IntersectionVertex iv2 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(42442273));
    IntersectionVertex iv3 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(1919595927));
    IntersectionVertex iv4 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(42452026));
    assertTrue(iv1.hasDrivingTrafficLight());
    assertTrue(iv2.hasDrivingTrafficLight());
    assertTrue(iv3.hasDrivingTrafficLight());
    assertTrue(iv4.hasDrivingTrafficLight());

    // These are not.
    IntersectionVertex iv5 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(42435485));
    IntersectionVertex iv6 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(42439335));
    IntersectionVertex iv7 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(42436761));
    IntersectionVertex iv8 = (IntersectionVertex) gg.getVertex(VertexLabel.osm(42442291));
    assertFalse(iv5.hasDrivingTrafficLight());
    assertFalse(iv6.hasDrivingTrafficLight());
    assertFalse(iv7.hasDrivingTrafficLight());
    assertFalse(iv8.hasDrivingTrafficLight());

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
    OsmWay way = new OsmWay();
    way.addTag("highway", "footway");
    way.addTag("cycleway", "lane");
    way.addTag("surface", "gravel");

    WayPropertySet wayPropertySet = new WayPropertySet();

    // where there are no way specifiers, the default is used
    var wayData = wayPropertySet.getDataForWay(way);
    assertEquals(ALL, wayData.forward().getPermission());
    assertEquals(1.0, wayData.forward().walkSafety());
    assertEquals(1.0, wayData.backward().walkSafety());
    assertEquals(1.0, wayData.forward().bicycleSafety());
    assertEquals(1.0, wayData.backward().bicycleSafety());

    // add two equal matches: lane only...
    OsmSpecifier lane_only = new BestMatchSpecifier("cycleway=lane");

    WayProperties lane_is_safer = withModes(ALL).bicycleSafety(1.5).walkSafety(1.0).build();

    wayPropertySet.addProperties(lane_only, lane_is_safer);

    // and footway only
    OsmSpecifier footway_only = new BestMatchSpecifier("highway=footway");

    WayProperties footways_allow_peds = new WayPropertiesBuilder(PEDESTRIAN).build();

    wayPropertySet.addProperties(footway_only, footways_allow_peds);

    var dataForWay = wayPropertySet.getDataForWay(way);
    // the first one is found
    assertEquals(lane_is_safer, dataForWay.forward());
    assertEquals(lane_is_safer, dataForWay.backward());

    // add a better match
    OsmSpecifier lane_and_footway = new BestMatchSpecifier("cycleway=lane;highway=footway");

    WayProperties safer_and_peds = new WayPropertiesBuilder(PEDESTRIAN)
      .bicycleSafety(0.75)
      .walkSafety(1.0)
      .build();

    wayPropertySet.addProperties(lane_and_footway, safer_and_peds);
    dataForWay = wayPropertySet.getDataForWay(way);
    assertEquals(new WayPropertiesPair(safer_and_peds, safer_and_peds), dataForWay);

    // add a mixin
    BestMatchSpecifier gravel = new BestMatchSpecifier("surface=gravel");
    var gravel_is_dangerous = MixinPropertiesBuilder.ofBicycleSafety(2);
    wayPropertySet.setMixinProperties(gravel, gravel_is_dangerous);

    dataForWay = wayPropertySet.getDataForWay(way);
    assertEquals(1.5, dataForWay.forward().bicycleSafety());

    // test a left-right distinction
    way = new OsmWay();
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
    // right (with traffic) comes from track
    assertEquals(0.25, dataForWay.forward().bicycleSafety());
    // left comes from lane
    assertEquals(0.75, dataForWay.backward().bicycleSafety());

    way = new OsmWay();
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
    OsmEntity way = new OsmWay();
    way.addTag("highway", "footway");
    way.addTag("cycleway", "lane");
    way.addTag("access", "no");

    CreativeNamer namer = new CreativeNamer(
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
    var service = new DefaultVehicleParkingService(buildParkingLots().repository);

    assertEquals(11, service.listVehicleParkings().size());
    assertEquals(6, service.listBikeParks().size());
    assertEquals(5, service.listCarParks().size());
  }

  @Test
  void createArtificalEntrancesToUnlikedParkingLots() {
    var graph = buildParkingLots().graph;

    graph
      .getVerticesOfType(VehicleParkingEntranceVertex.class)
      .stream()
      .filter(v -> v.getLabelString().contains("centroid"))
      .forEach(v -> {
        assertFalse(v.getOutgoing().isEmpty());
        assertFalse(v.getIncoming().isEmpty());
      });
  }

  /**
   * Test that a barrier vertex at ending street will get no access limit
   */
  @Test
  void testBarrierAtEnd() {
    var deduplicator = new Deduplicator();
    var graph = new Graph(deduplicator);

    File file = RESOURCE_LOADER.file("accessno-at-end.pbf");
    DefaultOsmProvider provider = new DefaultOsmProvider(file, false);
    OsmModule loader = OsmModule.of(
      provider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    ).build();
    loader.buildGraph();

    Vertex start = graph.getVertex(VertexLabel.osm(1));
    Vertex end = graph.getVertex(VertexLabel.osm(3));

    assertNotNull(start);
    assertNotNull(end);
    assertEquals(end.getClass(), BarrierVertex.class);
    var barrier = (BarrierVertex) end;

    // assert that pruning removed traversal restrictions
    assertEquals(barrier.getBarrierPermissions(), ALL);
  }

  @Test
  void testLinearCrossingNonIntersection() {
    var way = new OsmWay();
    way.addTag("highway", "path");
    way.addNodeRef(1);
    way.addNodeRef(2);
    way.addNodeRef(3);
    way.addNodeRef(4);
    way.setId(1);

    var barrier = new OsmWay();
    barrier.addTag("barrier", "fence");
    barrier.addNodeRef(99);
    barrier.addNodeRef(2);
    barrier.addNodeRef(98);
    barrier.setId(2);

    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(way, barrier),
      Set.of(1, 2, 3, 4, 98, 99)
        .stream()
        .map(id -> {
          var node = new OsmNode((double) id / 1000, 0);
          node.setId(id);
          return node;
        })
        .toList()
    );

    var graph = new Graph();
    var subject = OsmModule.of(
      osmProvider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    ).build();
    subject.buildGraph();

    assertEquals(3, graph.getVertices().size());
    var barrierVertices = graph.getVerticesOfType(BarrierVertex.class);
    assertEquals(1, barrierVertices.size());
    BarrierVertex barrierVertex = barrierVertices.getFirst();
    assertEquals(NONE, barrierVertex.getBarrierPermissions());
    assertEquals(NOT_POSSIBLE, barrierVertex.wheelchairAccessibility());
  }

  @Test
  void testHighwayReachingBarrierOnArea() {
    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0.001, 0);
    n2.setId(2);
    var n3 = new OsmNode(0, -0.001);
    n3.setId(3);
    var n4 = new OsmNode(0, 0.001);
    n4.setId(4);
    var n5 = new OsmNode(-0.001, 0.001);
    n5.setId(5);
    var n6 = new OsmNode(-0.001, -0.001);
    n6.setId(6);

    var path = new OsmWay();
    path.addTag("highway", "path");
    path.addNodeRef(1);
    path.addNodeRef(2);
    path.setId(1);

    var chain = new OsmWay();
    chain.addTag("barrier", "chain");
    chain.addNodeRef(3);
    chain.addNodeRef(1);
    chain.addNodeRef(4);
    chain.setId(2);

    var barrier = new OsmWay();
    barrier.addTag("highway", "pedestrian");
    barrier.addTag("bicycle", "yes");
    barrier.addTag("area", "yes");
    barrier.addNodeRef(1);
    barrier.addNodeRef(4);
    barrier.addNodeRef(5);
    barrier.addNodeRef(6);
    barrier.addNodeRef(3);
    barrier.addNodeRef(1);

    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(path, chain, barrier),
      List.of(n1, n2, n3, n4, n5, n6)
    );
    new OsmTagMapper().populateProperties(osmProvider.getWayPropertySet());

    var graph = new Graph();
    var subject = OsmModule.of(
      osmProvider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    ).build();
    subject.buildGraph();

    // the vertex for node 1 has been split, one for the area and one for the crossing of the linear
    // way
    Collection<Vertex> vertices = graph.getVertices();
    assertEquals(7, vertices.size());
    assertEquals(3, graph.getVerticesOfType(OsmVertexOnWay.class).size());
    assertEquals(1, graph.getVerticesOfType(BarrierVertex.class).size());
    assertEquals(
      2,
      graph.getVerticesOfType(OsmVertex.class).stream().filter(v -> v.nodeId == 1).toList().size()
    );

    // check traversal permission starting from node 2
    var v2 = graph
      .getVerticesOfType(OsmVertex.class)
      .stream()
      .filter(v -> v.nodeId == 2)
      .findFirst()
      .orElseThrow();
    assertEquals(1, v2.getOutgoing().size());
    // we first reach the barrier crossing on the path at v1
    var v1OnPath = v2.getOutgoingStreetEdges().getFirst().getToVertex();
    assertInstanceOf(BarrierVertex.class, v1OnPath);
    assertEquals(1, ((BarrierVertex) v1OnPath).nodeId);
    assertEquals(PEDESTRIAN, ((BarrierVertex) v1OnPath).getBarrierPermissions());
    // at that barrier crossing, we can either return to the origin or enter the area
    assertEquals(2, v1OnPath.getOutgoing().size());
    // we then enter the area
    var barrierCrossing = v1OnPath
      .getOutgoingStreetEdges()
      .stream()
      .filter(e -> e.getToVertex() instanceof OsmVertexOnWay)
      .findFirst()
      .orElseThrow();
    assertEquals(PEDESTRIAN, barrierCrossing.getPermission());
    var v1OnArea = barrierCrossing.getToVertex();
    for (var edge : v1OnArea.getOutgoingStreetEdges()) {
      // we check that we can cycle freely within the area, not encumbered by the chain
      if (edge instanceof AreaEdge) {
        assertEquals(PEDESTRIAN_AND_BICYCLE, edge.getPermission());
      }
    }
  }

  @Test
  void testDifferentLevelsConnectingBarrier() {
    Graph graph = new Graph();

    var n1 = new OsmNode(0, 0);
    n1.setId(1);
    var n2 = new OsmNode(0, 1);
    n2.setId(2);
    var n3 = new OsmNode(0, 2);
    n3.setId(3);
    var n4 = new OsmNode(0, 3);
    n4.setId(4);
    n4.addTag("barrier", "bollard");
    var n5 = new OsmNode(1, 0);
    n5.setId(5);
    var n6 = new OsmNode(-1, 0);
    n6.setId(6);
    n6.addTag("barrier", "bollard");

    var chain = new OsmWay();
    chain.addTag("barrier", "chain");
    chain.setId(999);
    chain.addNodeRef(1);
    chain.addNodeRef(2);
    chain.addNodeRef(3);

    var w1 = new OsmWay();
    w1.setId(1);
    w1.addTag("highway", "path");
    w1.addTag("level", "0");
    w1.addNodeRef(4);
    w1.addNodeRef(5);
    w1.addNodeRef(1);

    var w2 = new OsmWay();
    w2.setId(2);
    w2.addTag("highway", "pedestrian");
    w2.addTag("level", "1");
    w2.addTag("area", "yes");
    w2.addNodeRef(1);
    w2.addNodeRef(2);
    w2.addNodeRef(3);
    w2.addNodeRef(6);
    w2.addNodeRef(1);

    var w3 = new OsmWay();
    w3.setId(3);
    w3.addTag("highway", "path");
    w3.addTag("level", "1");
    w3.addNodeRef(4);
    w3.addNodeRef(6);
    w3.addNodeRef(1);

    var issueStore = new DefaultDataImportIssueStore();
    var osmProvider = new TestOsmProvider(
      List.of(),
      List.of(w1, w2, w3, chain),
      List.of(n1, n2, n3, n4, n5, n6)
    );
    var osmDb = new OsmDatabase(issueStore);
    osmProvider.readOsm(osmDb);

    var osmModule = OsmModule.of(
      osmProvider,
      graph,
      new DefaultOsmInfoGraphBuildRepository(),
      new DefaultVehicleParkingRepository()
    )
      .withIssueStore(issueStore)
      .build();
    osmModule.buildGraph();

    var issues = getBarrierLevelIssues(issueStore);
    assertEquals(2, issues.length);
    assertEquals(1, issues[0].node().getId());
    assertEquals(4, issues[1].node().getId());
  }

  private BuildResult buildParkingLots() {
    var graph = new Graph();
    var service = new DefaultVehicleParkingRepository();
    List<OsmProvider> providers = Stream.of("B+R.osm.pbf", "P+R.osm.pbf")
      .map(RESOURCE_LOADER::file)
      .map(f -> (OsmProvider) new DefaultOsmProvider(f, false))
      .toList();
    var module = OsmModule.of(providers, graph, new DefaultOsmInfoGraphBuildRepository(), service)
      .withStaticParkAndRide(true)
      .withStaticBikeParkAndRide(true)
      .build();
    module.buildGraph();
    return new BuildResult(graph, service);
  }

  private record BuildResult(Graph graph, VehicleParkingRepository repository) {}

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

    File file = RESOURCE_LOADER.file("usf_area.osm.pbf");
    var provider = new DefaultOsmProvider(file, false);
    var osmInfoRepository = new DefaultOsmInfoGraphBuildRepository();
    var vehicleParkingRepository = new DefaultVehicleParkingRepository();

    var loader = OsmModule.of(provider, graph, osmInfoRepository, vehicleParkingRepository)
      .withAreaVisibility(!skipVisibility)
      .build();

    loader.buildGraph();

    RouteRequest request = RouteRequest.defaultValue();

    //This are vertices that can be connected only over edges on area (with correct permissions)
    //It tests if it is possible to route over area without visibility calculations
    Vertex bottomV = graph.getVertex(VertexLabel.osm(580290955));
    Vertex topV = graph.getVertex(VertexLabel.osm(559271124));

    GraphPathFinder graphPathFinder = new GraphPathFinder(null);
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
