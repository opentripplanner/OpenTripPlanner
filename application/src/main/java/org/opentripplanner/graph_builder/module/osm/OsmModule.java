package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.NORMAL;
import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.SPLIT;
import static org.opentripplanner.osm.model.TraverseDirection.BACKWARD;
import static org.opentripplanner.osm.model.TraverseDirection.DIRECTIONLESS;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import gnu.trove.iterator.TLongIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.osm.edgelevelinfo.DefaultInclinedEdgeLevelInfoProcessor;
import org.opentripplanner.graph_builder.module.osm.edgelevelinfo.NoopInclinedEdgeLevelInfoProcessor;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmProcessingParameters;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.TraverseDirection;
import org.opentripplanner.osm.wayproperty.WayPropertiesPair;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.street.StreetRepository;
import org.opentripplanner.street.model.StreetModelDetails;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a street graph from OpenStreetMap data.
 */
public class OsmModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(OsmModule.class);

  private final Map<Vertex, Double> elevationData = new HashMap<>();

  /**
   * Providers of OSM data.
   */
  private final List<OsmProvider> providers;
  private final Graph graph;
  private final OsmInfoGraphBuildRepository osmInfoGraphBuildRepository;
  private final StreetDetailsRepository streetDetailsRepository;
  private final VehicleParkingRepository parkingRepository;
  private final StreetRepository streetRepository;

  private final DataImportIssueStore issueStore;
  private final OsmProcessingParameters params;
  private final SafetyValueNormalizer normalizer;

  OsmModule(
    Collection<OsmProvider> providers,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    StreetDetailsRepository streetDetailsRepository,
    VehicleParkingRepository parkingRepository,
    StreetRepository streetRepository,
    DataImportIssueStore issueStore,
    OsmProcessingParameters params
  ) {
    this.providers = List.copyOf(providers);
    this.graph = graph;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.streetDetailsRepository = streetDetailsRepository;
    this.parkingRepository = parkingRepository;
    this.streetRepository = streetRepository;
    this.issueStore = issueStore;
    this.params = params;
    this.normalizer = new SafetyValueNormalizer(graph, issueStore);
  }

  public static OsmModuleBuilder of(
    Collection<OsmProvider> providers,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    StreetDetailsRepository streetDetailsRepository,
    StreetRepository streetRepository,
    VehicleParkingRepository vehicleParkingRepository
  ) {
    return new OsmModuleBuilder(
      providers,
      graph,
      streetDetailsRepository,
      streetRepository,
      osmInfoGraphBuildRepository,
      vehicleParkingRepository
    );
  }

  @Override
  public void buildGraph() {
    // the OsmDatabase contains very large collections and should _not_ be stored as an instance
    // variable of this class, because this prevents it from being garbage collected at the end of
    // this method.
    var osmdb = new OsmDatabase(issueStore);
    var vertexGenerator = new VertexGenerator(
      osmdb,
      graph,
      params.boardingAreaRefTags(),
      params.includeOsmSubwayEntrances(),
      issueStore
    );
    for (var provider : providers) {
      LOG.info("Gathering OSM from provider: {}", provider);
      LOG.info(
        "Using OSM way configuration from {}.",
        provider.getOsmTagMapper().getClass().getSimpleName()
      );
      provider.readOsm(osmdb);
    }
    osmdb.postLoad();

    LOG.info("Building street graph from OSM");
    build(osmdb, vertexGenerator);
    graph.hasStreets = true;
    streetRepository.setStreetModelDetails(
      new StreetModelDetails(getMaxCarSpeed(), params.maxAreaNodes())
    );
    vertexGenerator.createDifferentLevelsSharingBarrierIssues();
  }

  @Override
  public void checkInputs() {
    for (var provider : providers) {
      provider.checkInputs();
    }
  }

  public Map<Vertex, Double> elevationDataOutput() {
    return elevationData;
  }

  private void build(OsmDatabase osmdb, VertexGenerator vertexGenerator) {
    var parkingProcessor = new ParkingProcessor(graph, issueStore, (node, way) ->
      vertexGenerator.getVertexForOsmNode(node, way, SPLIT)
    );

    var parkingLots = new ArrayList<VehicleParking>();

    if (params.staticParkAndRide()) {
      var carParkingNodes = parkingProcessor.buildParkAndRideNodes(
        osmdb.getCarParkingNodes(),
        true
      );
      parkingLots.addAll(carParkingNodes);
    }
    if (params.staticBikeParkAndRide()) {
      var bikeParkingNodes = parkingProcessor.buildParkAndRideNodes(
        osmdb.getBikeParkingNodes(),
        false
      );
      parkingLots.addAll(bikeParkingNodes);
    }

    for (OsmArea area : Iterables.concat(
      osmdb.getWalkableAreas(),
      osmdb.getParkAndRideAreas(),
      osmdb.getBikeParkingAreas()
    )) {
      setEntityName(area.parent);
    }

    // figure out which nodes that are actually intersections
    vertexGenerator.initIntersectionNodes();
    vertexGenerator.initNodesInBarrierWays();

    ElevatorProcessor elevatorProcessor = new ElevatorProcessor(
      issueStore,
      osmdb,
      vertexGenerator,
      graph,
      streetDetailsRepository
    );

    buildBasicGraph(osmdb, vertexGenerator, elevatorProcessor);
    buildWalkableAreas(osmdb, vertexGenerator, !params.areaVisibility());
    buildBarrierEdges(vertexGenerator);
    validateBarriers();

    if (params.staticParkAndRide()) {
      List<OsmAreaGroup> areaGroups = groupAreas(
        osmdb,
        osmdb.getParkAndRideAreas(),
        ImmutableMultimap.of()
      );
      var carParkingAreas = parkingProcessor.buildParkAndRideAreas(areaGroups);
      parkingLots.addAll(carParkingAreas);
      LOG.info("Created {} car P+R areas.", carParkingAreas.size());
    }
    if (params.staticBikeParkAndRide()) {
      List<OsmAreaGroup> areaGroups = groupAreas(
        osmdb,
        osmdb.getBikeParkingAreas(),
        ImmutableMultimap.of()
      );
      var bikeParkingAreas = parkingProcessor.buildBikeParkAndRideAreas(areaGroups);
      parkingLots.addAll(bikeParkingAreas);
      LOG.info("Created {} bike P+R areas", bikeParkingAreas.size());
    }

    if (!parkingLots.isEmpty()) {
      parkingRepository.updateVehicleParking(parkingLots, List.of());
    }

    elevatorProcessor.buildElevatorEdgesFromElevatorNodes();

    TurnRestrictionUnifier.unifyTurnRestrictions(osmdb, issueStore, osmInfoGraphBuildRepository);

    params.edgeNamer().finalizeNames();

    normalizer.applySafetyFactors();
  }

  /**
   * Returns the length of the geometry in meters.
   */
  private static double getGeometryLengthMeters(Geometry geometry) {
    Coordinate[] coordinates = geometry.getCoordinates();
    double d = 0;
    for (int i = 1; i < coordinates.length; ++i) {
      d += SphericalDistanceLibrary.distance(coordinates[i - 1], coordinates[i]);
    }
    return d;
  }

  private List<OsmAreaGroup> groupAreas(
    OsmDatabase osmdb,
    Collection<OsmArea> areas,
    Multimap<OsmNode, OsmWay> barriers
  ) {
    Map<OsmArea, Set<OsmLevel>> areasLevels = new HashMap<>(areas.size());
    for (OsmArea area : areas) {
      areasLevels.put(area, osmdb.getLevelSetForEntity(area.parent));
    }
    return OsmAreaGroup.groupAreas(areasLevels, barriers);
  }

  private void buildWalkableAreas(
    OsmDatabase osmdb,
    VertexGenerator vertexGenerator,
    boolean skipVisibility
  ) {
    if (skipVisibility) {
      LOG.info(
        "Skipping visibility graph construction for walkable areas and using just area rings for edges."
      );
    } else {
      LOG.info("Building visibility graphs for walkable areas.");
    }
    List<OsmAreaGroup> areaGroups = groupAreas(
      osmdb,
      osmdb.getWalkableAreas(),
      vertexGenerator.nodesInBarrierWays()
    );
    WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(
      graph,
      osmdb,
      osmInfoGraphBuildRepository,
      vertexGenerator,
      params.edgeNamer(),
      normalizer,
      issueStore,
      params.maxAreaNodes(),
      params.platformEntriesLinking(),
      params.boardingAreaRefTags()
    );
    if (skipVisibility) {
      for (OsmAreaGroup group : areaGroups) {
        walkableAreaBuilder.buildWithoutVisibility(group);
      }
    } else {
      ProgressTracker progress = ProgressTracker.track(
        "Build visibility graph for areas",
        50,
        areaGroups.size()
      );
      for (OsmAreaGroup group : areaGroups) {
        walkableAreaBuilder.buildWithVisibility(group);
        //Keep lambda! A method-ref would log incorrect class and line number
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      }
      LOG.info(progress.completeMessage());
    }

    if (skipVisibility) {
      LOG.info("Done building rings for walkable areas.");
    } else {
      LOG.info("Done building visibility graphs for walkable areas.");
    }
  }

  private void buildBasicGraph(
    OsmDatabase osmdb,
    VertexGenerator vertexGenerator,
    ElevatorProcessor elevatorProcessor
  ) {
    /* build the street segment graph from OSM ways */
    long wayCount = osmdb.getWays().size();
    ProgressTracker progress = ProgressTracker.track("Build street graph", 5_000, wayCount);
    LOG.info(progress.startMessage());
    var escalatorProcessor = new EscalatorProcessor(issueStore);
    var inclinedEdgeLevelInfoProcessor = params.includeInclinedEdgeLevelInfo()
      ? new DefaultInclinedEdgeLevelInfoProcessor(issueStore, streetDetailsRepository, osmdb)
      : new NoopInclinedEdgeLevelInfoProcessor();

    WAY: for (OsmWay way : osmdb.getWays()) {
      WayPropertiesPair wayData = way.getOsmProvider().getWayPropertySet().getDataForWay(way);
      setEntityName(way);

      var forwardPermission = wayData.forward().getPermission();
      var backwardPermission = wayData.backward().getPermission();

      if (
        !way.isRoutable() ||
        (forwardPermission.allowsNothing() && backwardPermission.allowsNothing())
      ) {
        continue;
      }

      // handle duplicate nodes in OSM ways
      // this is a workaround for crappy OSM data quality
      ArrayList<Long> nodes = new ArrayList<>(way.getNodeRefs().size());
      long last = -1;
      double lastLat = -1;
      double lastLon = -1;
      String lastLevel = null;
      for (TLongIterator iter = way.getNodeRefs().iterator(); iter.hasNext();) {
        long nodeId = iter.next();
        OsmNode node = osmdb.getNode(nodeId);
        if (node == null) {
          continue WAY;
        }
        boolean levelsDiffer = false;
        String level = node.getTag("level");
        if (lastLevel == null) {
          if (level != null) {
            levelsDiffer = true;
          }
        } else {
          if (!lastLevel.equals(level)) {
            levelsDiffer = true;
          }
        }
        if (nodeId != last && (node.lat != lastLat || node.lon != lastLon || levelsDiffer)) {
          nodes.add(nodeId);
        }
        last = nodeId;
        lastLon = node.lon;
        lastLat = node.lat;
        lastLevel = level;
      }

      IntersectionVertex fromVertex = null;
      IntersectionVertex toVertex = null;

      ArrayList<Coordinate> segmentCoordinates = new ArrayList<>();

      /*
       * Traverse through all the nodes of this edge. For nodes which are not shared with any other edge, do not create endpoints -- just
       * accumulate them for geometry and ele tags. For nodes which are shared, create endpoints and StreetVertex instances. One exception:
       * if the next vertex also appears earlier in the way, we need to split the way, because otherwise we have a way that loops from a
       * vertex to itself, which could cause issues with splitting.
       */
      Long startNode = null;
      // where the current edge should start
      OsmNode osmStartNode = null;

      var platformOptional = getPlatform(osmdb, way);
      var inclinedEdgeLevelInfoOptional = inclinedEdgeLevelInfoProcessor.findInclinedEdgeLevelInfo(
        way
      );

      for (int i = 0; i < nodes.size() - 1; i++) {
        OsmNode segmentStartOsmNode = osmdb.getNode(nodes.get(i));

        if (segmentStartOsmNode == null) {
          continue;
        }

        Long endNode = nodes.get(i + 1);

        if (osmStartNode == null) {
          startNode = nodes.get(i);
          osmStartNode = segmentStartOsmNode;
        }
        // where the current edge might end
        OsmNode osmEndNode = osmdb.getNode(endNode);

        LineString geometry;

        /*
         * We split segments at intersections, self-intersections, nodes with ele tags, and transit stops;
         * the only processing we do on other nodes is to accumulate their geometry
         */
        if (segmentCoordinates.isEmpty()) {
          segmentCoordinates.add(osmStartNode.getCoordinate());
        }

        if (
          vertexGenerator.intersectionNodes().containsKey(endNode) ||
          i == nodes.size() - 2 ||
          nodes.subList(0, i).contains(nodes.get(i)) ||
          osmEndNode.hasTag("ele") ||
          osmEndNode.isBoardingLocation() ||
          osmEndNode.isBarrier() ||
          vertexGenerator.nodesInBarrierWays().containsKey(osmEndNode)
        ) {
          segmentCoordinates.add(osmEndNode.getCoordinate());

          geometry = GeometryUtils.getGeometryFactory()
            .createLineString(segmentCoordinates.toArray(new Coordinate[0]));
          segmentCoordinates.clear();
        } else {
          segmentCoordinates.add(osmEndNode.getCoordinate());
          continue;
        }

        /* generate endpoints */
        if (fromVertex == null) { // first iteration on this way
          // make or get a shared vertex for flat intersections,
          // one vertex per level for multilevel nodes like elevators
          fromVertex = vertexGenerator.getVertexForOsmNode(osmStartNode, way, NORMAL);
          String ele = segmentStartOsmNode.getTag("ele");
          if (ele != null) {
            Double elevation = ElevationUtils.parseEleTag(ele);
            if (elevation != null) {
              elevationData.put(fromVertex, elevation);
            }
          }
        } else { // subsequent iterations
          fromVertex = toVertex;
        }

        toVertex = vertexGenerator.getVertexForOsmNode(osmEndNode, way, NORMAL);
        String ele = osmEndNode.getTag("ele");
        if (ele != null) {
          Double elevation = ElevationUtils.parseEleTag(ele);
          if (elevation != null) {
            elevationData.put(toVertex, elevation);
          }
        }
        if (way.isEscalator()) {
          var length = getGeometryLengthMeters(geometry);
          EscalatorEdgePair escalatorEdgePair = escalatorProcessor.buildEscalatorEdge(
            way,
            length,
            fromVertex,
            toVertex
          );
          if (inclinedEdgeLevelInfoOptional.isPresent()) {
            inclinedEdgeLevelInfoProcessor.storeLevelInfoForEdge(
              escalatorEdgePair.main(),
              escalatorEdgePair.back(),
              inclinedEdgeLevelInfoOptional.get(),
              way
            );
          }
        } else if (elevatorProcessor.isElevatorWay(way)) {
          elevatorProcessor.buildElevatorEdgesFromElevatorWay(way);
        } else {
          StreetEdgePair streets = getEdgesForStreet(
            fromVertex,
            toVertex,
            way,
            i,
            forwardPermission,
            backwardPermission,
            geometry
          );

          params.edgeNamer().recordEdges(way, streets, osmdb);

          StreetEdge street = streets.main();
          StreetEdge backStreet = streets.back();
          normalizer.applyWayProperties(
            street,
            backStreet,
            wayData.forward(),
            wayData.backward(),
            way
          );

          platformOptional.ifPresent(plat -> {
            for (var s : streets.asIterable()) {
              osmInfoGraphBuildRepository.addPlatform(s, plat);
            }
          });

          if (way.isStairs() && inclinedEdgeLevelInfoOptional.isPresent()) {
            inclinedEdgeLevelInfoProcessor.storeLevelInfoForEdge(
              street,
              backStreet,
              inclinedEdgeLevelInfoOptional.get(),
              way
            );
          }

          applyEdgesToTurnRestrictions(osmdb, way, startNode, endNode, street, backStreet);
          startNode = endNode;
          osmStartNode = osmdb.getNode(startNode);
        }
      }

      //Keep lambda! A method-ref would log incorrect class and line number
      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    } // END loop over OSM ways

    LOG.info(progress.completeMessage());
  }

  private void buildBarrierEdges(VertexGenerator vertexGenerator) {
    var barrierEdgeBuilder = new BarrierEdgeBuilder(params.edgeNamer());
    LOG.info("Building edges to pass through linear barriers");
    var verticesGroups = vertexGenerator.splitVerticesOnBarriers();
    ProgressTracker progress = ProgressTracker.track(
      "Build edges through barriers",
      50,
      verticesGroups.size()
    );
    for (var item : verticesGroups.entrySet()) {
      barrierEdgeBuilder.build(
        item.getKey(),
        item.getValue().values(),
        vertexGenerator.getLinearBarriersAtNode(item.getKey())
      );

      //Keep lambda! A method-ref would log incorrect class and line number
      //noinspection Convert2MethodRef
      progress.step(m -> LOG.info(m));
    }
    LOG.info(progress.completeMessage());
    LOG.info("Complete building edges through linear barriers");
  }

  private Optional<Platform> getPlatform(OsmDatabase osmdb, OsmWay way) {
    var references = way.getMultiTagValues(params.boardingAreaRefTags());
    if (way.isBoardingLocation() && !references.isEmpty()) {
      var nodeRefs = way.getNodeRefs();
      var size = nodeRefs.size();
      var nodes = new Coordinate[size];
      for (int i = 0; i < size; i++) {
        nodes[i] = osmdb.getNode(nodeRefs.get(i)).getCoordinate();
      }

      var geometryFactory = GeometryUtils.getGeometryFactory();

      var geometry = geometryFactory.createLineString(nodes);

      return Optional.of(
        new Platform(
          params.edgeNamer().getName(way, "platform " + way.getId()),
          geometry,
          references
        )
      );
    } else {
      return Optional.empty();
    }
  }

  private void validateBarriers() {
    List<BarrierVertex> vertices = graph.getVerticesOfType(BarrierVertex.class);
    vertices.forEach(bv -> bv.makeBarrierAtEndReachable());
  }

  private void setEntityName(OsmEntity entity) {
    if (!entity.hasTag("name")) {
      I18NString creativeName = entity.getOsmProvider().getWayPropertySet().getCreativeName(entity);
      if (creativeName != null) {
        entity.setCreativeName(creativeName);
      }
    }
  }

  private void applyEdgesToTurnRestrictions(
    OsmDatabase osmdb,
    OsmWay way,
    long startNode,
    long endNode,
    @Nullable StreetEdge street,
    @Nullable StreetEdge backStreet
  ) {
    /* Check if there are turn restrictions starting on this segment */
    Collection<TurnRestrictionTag> restrictionTags = osmdb.getFromWayTurnRestrictions(way.getId());

    if (restrictionTags != null) {
      for (TurnRestrictionTag tag : restrictionTags) {
        if (tag.via == startNode) {
          tag.possibleFrom.add(backStreet);
        } else if (tag.via == endNode) {
          tag.possibleFrom.add(street);
        }
      }
    }

    restrictionTags = osmdb.getToWayTurnRestrictions(way.getId());
    if (restrictionTags != null) {
      for (TurnRestrictionTag tag : restrictionTags) {
        if (tag.via == startNode) {
          tag.possibleTo.add(street);
        } else if (tag.via == endNode) {
          tag.possibleTo.add(backStreet);
        }
      }
    }
  }

  /**
   * Handle oneway streets, cycleways, and other per-mode and universal access controls. See
   * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios, along with
   * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
   */
  private StreetEdgePair getEdgesForStreet(
    IntersectionVertex fromVertex,
    IntersectionVertex toVertex,
    OsmWay way,
    int index,
    StreetTraversalPermission forwardPermission,
    StreetTraversalPermission backwardPermission,
    LineString geometry
  ) {
    // No point in returning edges that can't be traversed by anyone.
    if (forwardPermission.allowsNothing() && backwardPermission.allowsNothing()) {
      return new StreetEdgePair(null, null);
    }

    LineString backGeometry = geometry.reverse();
    StreetEdge street = null;
    StreetEdge backStreet = null;
    double length = getGeometryLengthMeters(geometry);

    if (forwardPermission.allowsAnything()) {
      street = getEdgeForStreet(
        fromVertex,
        toVertex,
        way,
        index,
        length,
        forwardPermission,
        geometry,
        FORWARD
      );
    }
    if (backwardPermission.allowsAnything()) {
      backStreet = getEdgeForStreet(
        toVertex,
        fromVertex,
        way,
        index,
        length,
        backwardPermission,
        backGeometry,
        BACKWARD
      );
    }
    if (street != null && backStreet != null) {
      backStreet.shareData(street);
    }
    return new StreetEdgePair(street, backStreet);
  }

  private StreetEdge getEdgeForStreet(
    IntersectionVertex fromVertex,
    IntersectionVertex toVertex,
    OsmWay way,
    int index,
    double length,
    StreetTraversalPermission permissions,
    LineString geometry,
    TraverseDirection direction
  ) {
    if (direction == DIRECTIONLESS) {
      throw new IllegalArgumentException(
        "A direction must be specified when getting an edge for a street."
      );
    }

    String label = "way " + way.getId() + " from " + index;
    label = label.intern();
    I18NString name = params.edgeNamer().getName(way, label);
    float carSpeed = way.getOsmProvider().getOsmTagMapper().getCarSpeedForWay(way, direction);

    StreetEdgeBuilder<?> seb = new StreetEdgeBuilder<>()
      .withFromVertex(fromVertex)
      .withToVertex(toVertex)
      .withGeometry(geometry)
      .withName(name)
      .withMeterLength(length)
      .withPermission(permissions)
      .withBack(direction == BACKWARD)
      .withCarSpeed(carSpeed)
      .withLink(way.isLink())
      .withRoundabout(way.isRoundabout())
      .withCrossing(way.isCrossing())
      .withSlopeOverride(way.getOsmProvider().getWayPropertySet().getSlopeOverride(way))
      .withStairs(way.isStairs())
      .withWheelchairAccessible(way.isWheelchairAccessible())
      .withBogusName(way.hasNoName());

    return seb.buildAndConnect();
  }

  private float getMaxCarSpeed() {
    float maxSpeed = 0f;
    for (var provider : providers) {
      var carSpeed = provider.getOsmTagMapper().getMaxUsedCarSpeed(provider.getWayPropertySet());
      if (carSpeed > maxSpeed) {
        maxSpeed = carSpeed;
      }
    }
    return maxSpeed;
  }
}
