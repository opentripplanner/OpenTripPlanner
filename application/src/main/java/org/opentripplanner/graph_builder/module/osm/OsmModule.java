package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.Iterables;
import gnu.trove.iterator.TLongIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmProcessingParameters;
import org.opentripplanner.osm.DefaultOsmProvider;
import org.opentripplanner.osm.OsmProvider;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.model.Platform;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.street.model.StreetLimitationParameters;
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
  private final VehicleParkingRepository parkingRepository;

  private final DataImportIssueStore issueStore;
  private final OsmProcessingParameters params;
  private final SafetyValueNormalizer normalizer;
  private final VertexGenerator vertexGenerator;
  private final OsmDatabase osmdb;
  private final StreetLimitationParameters streetLimitationParameters;

  OsmModule(
    Collection<OsmProvider> providers,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VehicleParkingRepository parkingRepository,
    DataImportIssueStore issueStore,
    StreetLimitationParameters streetLimitationParameters,
    OsmProcessingParameters params
  ) {
    this.providers = List.copyOf(providers);
    this.graph = graph;
    this.osmInfoGraphBuildRepository = osmInfoGraphBuildRepository;
    this.parkingRepository = parkingRepository;
    this.issueStore = issueStore;
    this.params = params;
    this.osmdb = new OsmDatabase(issueStore);
    this.vertexGenerator = new VertexGenerator(
      osmdb,
      graph,
      params.boardingAreaRefTags(),
      params.includeOsmSubwayEntrances()
    );
    this.normalizer = new SafetyValueNormalizer(graph, issueStore);
    this.streetLimitationParameters = Objects.requireNonNull(streetLimitationParameters);
  }

  public static OsmModuleBuilder of(
    Collection<OsmProvider> providers,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VehicleParkingRepository vehicleParkingRepository
  ) {
    return new OsmModuleBuilder(
      providers,
      graph,
      osmInfoGraphBuildRepository,
      vehicleParkingRepository
    );
  }

  public static OsmModuleBuilder of(
    OsmProvider provider,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    VehicleParkingRepository vehicleParkingRepository
  ) {
    return of(List.of(provider), graph, osmInfoGraphBuildRepository, vehicleParkingRepository);
  }

  @Override
  public void buildGraph() {
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
    build();
    graph.hasStreets = true;
    streetLimitationParameters.initMaxCarSpeed(getMaxCarSpeed());
    streetLimitationParameters.initMaxAreaNodes(params.maxAreaNodes());
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

  private void build() {
    var parkingProcessor = new ParkingProcessor(
      graph,
      issueStore,
      vertexGenerator::getVertexForOsmNode
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
    )) setWayName(area.parent);

    // figure out which nodes that are actually intersections
    vertexGenerator.initIntersectionNodes();

    buildBasicGraph();
    buildWalkableAreas(!params.areaVisibility());
    validateBarriers();

    if (params.staticParkAndRide()) {
      List<OsmAreaGroup> areaGroups = groupAreas(osmdb.getParkAndRideAreas());
      var carParkingAreas = parkingProcessor.buildParkAndRideAreas(areaGroups);
      parkingLots.addAll(carParkingAreas);
      LOG.info("Created {} car P+R areas.", carParkingAreas.size());
    }
    if (params.staticBikeParkAndRide()) {
      List<OsmAreaGroup> areaGroups = groupAreas(osmdb.getBikeParkingAreas());
      var bikeParkingAreas = parkingProcessor.buildBikeParkAndRideAreas(areaGroups);
      parkingLots.addAll(bikeParkingAreas);
      LOG.info("Created {} bike P+R areas", bikeParkingAreas.size());
    }

    if (!parkingLots.isEmpty()) {
      parkingRepository.updateVehicleParking(parkingLots, List.of());
    }

    var elevatorProcessor = new ElevatorProcessor(issueStore, osmdb, vertexGenerator);
    elevatorProcessor.buildElevatorEdges(graph);

    TurnRestrictionUnifier.unifyTurnRestrictions(osmdb, issueStore, osmInfoGraphBuildRepository);

    params.edgeNamer().postprocess();

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

  private List<OsmAreaGroup> groupAreas(Collection<OsmArea> areas) {
    Map<OsmArea, OsmLevel> areasLevels = new HashMap<>(areas.size());
    for (OsmArea area : areas) {
      areasLevels.put(area, osmdb.getLevelForWay(area.parent));
    }
    return OsmAreaGroup.groupAreas(areasLevels);
  }

  private void buildWalkableAreas(boolean skipVisibility) {
    if (skipVisibility) {
      LOG.info(
        "Skipping visibility graph construction for walkable areas and using just area rings for edges."
      );
    } else {
      LOG.info("Building visibility graphs for walkable areas.");
    }
    List<OsmAreaGroup> areaGroups = groupAreas(osmdb.getWalkableAreas());
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

  private void buildBasicGraph() {
    /* build the street segment graph from OSM ways */
    long wayCount = osmdb.getWays().size();
    ProgressTracker progress = ProgressTracker.track("Build street graph", 5_000, wayCount);
    LOG.info(progress.startMessage());
    var escalatorProcessor = new EscalatorProcessor(
      vertexGenerator.intersectionNodes(),
      issueStore
    );

    WAY: for (OsmWay way : osmdb.getWays()) {
      WayProperties wayData = way.getOsmProvider().getWayPropertySet().getDataForWay(way);
      setWayName(way);

      var permissions = wayData.getPermission();

      if (!way.isRoutable() || permissions.allowsNothing()) {
        continue;
      }

      // handle duplicate nodes in OSM ways
      // this is a workaround for crappy OSM data quality
      ArrayList<Long> nodes = new ArrayList<>(way.getNodeRefs().size());
      long last = -1;
      double lastLat = -1, lastLon = -1;
      String lastLevel = null;
      for (TLongIterator iter = way.getNodeRefs().iterator(); iter.hasNext();) {
        long nodeId = iter.next();
        OsmNode node = osmdb.getNode(nodeId);
        if (node == null) continue WAY;
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
        if (
          nodeId != last && (node.lat != lastLat || node.lon != lastLon || levelsDiffer)
        ) nodes.add(nodeId);
        last = nodeId;
        lastLon = node.lon;
        lastLat = node.lat;
        lastLevel = level;
      }

      IntersectionVertex startEndpoint = null;
      IntersectionVertex endEndpoint = null;

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

      var platform = getPlatform(way);

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
          osmEndNode.isBarrier()
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
        if (startEndpoint == null) { // first iteration on this way
          // make or get a shared vertex for flat intersections,
          // one vertex per level for multilevel nodes like elevators
          startEndpoint = vertexGenerator.getVertexForOsmNode(osmStartNode, way);
          String ele = segmentStartOsmNode.getTag("ele");
          if (ele != null) {
            Double elevation = ElevationUtils.parseEleTag(ele);
            if (elevation != null) {
              elevationData.put(startEndpoint, elevation);
            }
          }
        } else { // subsequent iterations
          startEndpoint = endEndpoint;
        }

        endEndpoint = vertexGenerator.getVertexForOsmNode(osmEndNode, way);
        String ele = osmEndNode.getTag("ele");
        if (ele != null) {
          Double elevation = ElevationUtils.parseEleTag(ele);
          if (elevation != null) {
            elevationData.put(endEndpoint, elevation);
          }
        }
        if (way.isEscalator()) {
          var length = getGeometryLengthMeters(geometry);
          escalatorProcessor.buildEscalatorEdge(way, length);
        } else {
          StreetEdgePair streets = getEdgesForStreet(
            startEndpoint,
            endEndpoint,
            way,
            i,
            permissions,
            geometry
          );

          params.edgeNamer().recordEdges(way, streets);

          StreetEdge street = streets.main();
          StreetEdge backStreet = streets.back();
          normalizer.applyWayProperties(street, backStreet, wayData, way);

          platform.ifPresent(plat -> {
            for (var s : streets.asIterable()) {
              osmInfoGraphBuildRepository.addPlatform(s, plat);
            }
          });

          applyEdgesToTurnRestrictions(way, startNode, endNode, street, backStreet);
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

  private Optional<Platform> getPlatform(OsmWay way) {
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
          params.edgeNamer().getNameForWay(way, "platform " + way.getId()),
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

  private void setWayName(OsmEntity way) {
    if (!way.hasTag("name")) {
      I18NString creativeName = way.getOsmProvider().getWayPropertySet().getCreativeNameForWay(way);
      if (creativeName != null) {
        way.setCreativeName(creativeName);
      }
    }
  }

  private void applyEdgesToTurnRestrictions(
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
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    OsmWay way,
    int index,
    StreetTraversalPermission permissions,
    LineString geometry
  ) {
    // No point in returning edges that can't be traversed by anyone.
    if (permissions.allowsNothing()) {
      return new StreetEdgePair(null, null);
    }

    LineString backGeometry = geometry.reverse();
    StreetEdge street = null;
    StreetEdge backStreet = null;
    double length = getGeometryLengthMeters(geometry);

    var permissionPair = way.splitPermissions(permissions);
    var permissionsFront = permissionPair.main();
    var permissionsBack = permissionPair.back();

    if (permissionsFront.allowsAnything()) {
      street = getEdgeForStreet(
        startEndpoint,
        endEndpoint,
        way,
        index,
        length,
        permissionsFront,
        geometry,
        false
      );
    }
    if (permissionsBack.allowsAnything()) {
      backStreet = getEdgeForStreet(
        endEndpoint,
        startEndpoint,
        way,
        index,
        length,
        permissionsBack,
        backGeometry,
        true
      );
    }
    if (street != null && backStreet != null) {
      backStreet.shareData(street);
    }
    return new StreetEdgePair(street, backStreet);
  }

  private StreetEdge getEdgeForStreet(
    IntersectionVertex startEndpoint,
    IntersectionVertex endEndpoint,
    OsmWay way,
    int index,
    double length,
    StreetTraversalPermission permissions,
    LineString geometry,
    boolean back
  ) {
    String label = "way " + way.getId() + " from " + index;
    label = label.intern();
    I18NString name = params.edgeNamer().getNameForWay(way, label);
    float carSpeed = way.getOsmProvider().getOsmTagMapper().getCarSpeedForWay(way, back);

    StreetEdgeBuilder<?> seb = new StreetEdgeBuilder<>()
      .withFromVertex(startEndpoint)
      .withToVertex(endEndpoint)
      .withGeometry(geometry)
      .withName(name)
      .withMeterLength(length)
      .withPermission(permissions)
      .withBack(back)
      .withCarSpeed(carSpeed)
      .withLink(way.isLink())
      .withRoundabout(way.isRoundabout())
      .withSlopeOverride(way.getOsmProvider().getWayPropertySet().getSlopeOverride(way))
      .withStairs(way.isSteps())
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
