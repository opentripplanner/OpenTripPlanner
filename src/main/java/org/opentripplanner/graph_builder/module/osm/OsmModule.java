package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.Iterables;
import gnu.trove.iterator.TLongIterator;
import gnu.trove.list.TLongList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.logging.ProgressTracker;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.Graphwide;
import org.opentripplanner.graph_builder.issues.StreetCarSpeedZero;
import org.opentripplanner.graph_builder.issues.TurnRestrictionBad;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmProcessingParameters;
import org.opentripplanner.openstreetmap.OsmProvider;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapper;
import org.opentripplanner.openstreetmap.wayproperty.WayProperties;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.util.ElevationUtils;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.edge.AreaEdge;
import org.opentripplanner.street.model.edge.AreaEdgeList;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.NamedArea;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.note.StreetNoteAndMatcher;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.ExitVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a street graph from OpenStreetMap data.
 */
public class OsmModule implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(OsmModule.class);

  private final Map<Vertex, Double> elevationData = new HashMap<>();

  // Private members that are only read or written internally.
  /**
   * Providers of OSM data.
   */
  private final List<OsmProvider> providers;
  private final Graph graph;
  private final DataImportIssueStore issueStore;
  private final OsmProcessingParameters options;

  public OsmModule(
    Collection<OsmProvider> providers,
    Graph graph,
    DataImportIssueStore issueStore,
    OsmProcessingParameters options
  ) {
    this.providers = List.copyOf(providers);
    this.graph = graph;
    this.issueStore = issueStore;
    this.options = options;
  }

  public static OsmModuleBuilder of(Collection<OsmProvider> providers, Graph graph) {
    return new OsmModuleBuilder(providers, graph);
  }

  public static OsmModuleBuilder of(OsmProvider provider, Graph graph) {
    return of(List.of(provider), graph);
  }

  @Override
  public void buildGraph() {
    OsmDatabase osmdb = new OsmDatabase(issueStore, options.boardingAreaRefTags());
    Handler handler = new Handler(graph, osmdb, issueStore, options, elevationData);
    for (OsmProvider provider : providers) {
      LOG.info("Gathering OSM from provider: {}", provider);
      LOG.info(
        "Using OSM way configuration from {}.",
        provider.getOsmTagMapper().getClass().getSimpleName()
      );
      provider.readOSM(osmdb);
    }
    osmdb.postLoad();

    LOG.info("Building street graph from OSM");
    handler.buildGraph();
    graph.hasStreets = true;
  }

  @Override
  public void checkInputs() {
    for (OsmProvider provider : providers) {
      provider.checkInputs();
    }
  }

  public Map<Vertex, Double> elevationDataOutput() {
    return elevationData;
  }

  private record StreetEdgePair(StreetEdge main, StreetEdge back) {}

  protected static class Handler {

    private static final String nodeLabelFormat = "osm:node:%d";

    private static final String levelnodeLabelFormat = nodeLabelFormat + ":level:%s";

    private final Graph graph;

    private final OsmDatabase osmdb;
    private final DataImportIssueStore issueStore;
    private final Map<Vertex, Double> elevationData;

    // track OSM nodes which are decomposed into multiple graph vertices because they are
    // elevators. later they will be iterated over to build ElevatorEdges between them.
    private final HashMap<Long, Map<OSMLevel, OsmVertex>> multiLevelNodes = new HashMap<>();
    // track OSM nodes that will become graph vertices because they appear in multiple OSM ways
    private final Map<Long, IntersectionVertex> intersectionNodes = new HashMap<>();
    private final OsmProcessingParameters options;
    /**
     * The bike safety factor of the safest street
     */
    private float bestBikeSafety = 1.0f;
    /**
     * The walk safety factor of the safest street
     */
    private float bestWalkSafety = 1.0f;

    // TODO Decouple this class further, decrease the number of constructor parameters.
    public Handler(
      Graph graph,
      OsmDatabase osmdb,
      DataImportIssueStore issueStore,
      OsmProcessingParameters options,
      Map<Vertex, Double> elevationData
    ) {
      this.graph = graph;
      this.osmdb = osmdb;
      this.issueStore = issueStore;
      this.options = options;
      this.elevationData = elevationData;
    }

    public void buildGraph() {
      var parkingProcessor = new ParkingProcessor(graph, issueStore, this::getVertexForOsmNode);

      var parkingLots = new ArrayList<VehicleParking>();

      if (options.staticParkAndRide()) {
        var carParkingNodes = parkingProcessor.buildParkAndRideNodes(
          osmdb.getCarParkingNodes(),
          true
        );
        parkingLots.addAll(carParkingNodes);
      }
      if (options.staticBikeParkAndRide()) {
        var bikeParkingNodes = parkingProcessor.buildParkAndRideNodes(
          osmdb.getBikeParkingNodes(),
          false
        );
        parkingLots.addAll(bikeParkingNodes);
      }

      for (Area area : Iterables.concat(
        osmdb.getWalkableAreas(),
        osmdb.getParkAndRideAreas(),
        osmdb.getBikeParkingAreas()
      )) setWayName(area.parent);

      // figure out which nodes that are actually intersections
      initIntersectionNodes();

      buildBasicGraph();
      buildWalkableAreas(!options.areaVisibility(), options.platformEntriesLinking());

      if (options.staticParkAndRide()) {
        List<AreaGroup> areaGroups = groupAreas(osmdb.getParkAndRideAreas());
        var carParkingAreas = parkingProcessor.buildParkAndRideAreas(areaGroups);
        parkingLots.addAll(carParkingAreas);
        LOG.info("Created {} car P+R areas.", carParkingAreas.size());
      }
      if (options.staticBikeParkAndRide()) {
        List<AreaGroup> areaGroups = groupAreas(osmdb.getBikeParkingAreas());
        var bikeParkingAreas = parkingProcessor.buildBikeParkAndRideAreas(areaGroups);
        parkingLots.addAll(bikeParkingAreas);
        LOG.info("Created {} bike P+R areas", bikeParkingAreas.size());
      }

      if (!parkingLots.isEmpty()) {
        graph.getVehicleParkingService().updateVehicleParking(parkingLots, List.of());
      }

      var elevatorProcessor = new ElevatorProcessor(
        issueStore,
        osmdb,
        multiLevelNodes,
        intersectionNodes
      );
      elevatorProcessor.buildElevatorEdges(graph);

      unifyTurnRestrictions();

      if (options.customNamer() != null) {
        options.customNamer().postprocess(graph);
      }

      applySafetyFactors(graph);
    }

    // TODO Set this to private once WalkableAreaBuilder is gone
    protected void applyWayProperties(
      StreetEdge street,
      StreetEdge backStreet,
      WayProperties wayData,
      OSMWithTags way
    ) {
      OsmTagMapper tagMapperForWay = way.getOsmProvider().getOsmTagMapper();

      Set<StreetNoteAndMatcher> notes = way.getOsmProvider().getWayPropertySet().getNoteForWay(way);

      boolean motorVehicleNoThrough = tagMapperForWay.isMotorVehicleThroughTrafficExplicitlyDisallowed(
        way
      );
      boolean bicycleNoThrough = tagMapperForWay.isBicycleNoThroughTrafficExplicitlyDisallowed(way);
      boolean walkNoThrough = tagMapperForWay.isWalkNoThroughTrafficExplicitlyDisallowed(way);

      if (street != null) {
        double bicycleSafety = wayData.getBicycleSafetyFeatures().forward();
        street.setBicycleSafetyFactor((float) bicycleSafety);
        if (bicycleSafety < bestBikeSafety) {
          bestBikeSafety = (float) bicycleSafety;
        }
        double walkSafety = wayData.getWalkSafetyFeatures().forward();
        street.setWalkSafetyFactor((float) walkSafety);
        if (walkSafety < bestWalkSafety) {
          bestWalkSafety = (float) walkSafety;
        }
        if (notes != null) {
          for (var it : notes) {
            graph.streetNotesService.addStaticNote(street, it.note(), it.matcher());
          }
        }
        street.setMotorVehicleNoThruTraffic(motorVehicleNoThrough);
        street.setBicycleNoThruTraffic(bicycleNoThrough);
        street.setWalkNoThruTraffic(walkNoThrough);
      }

      if (backStreet != null) {
        double bicycleSafety = wayData.getBicycleSafetyFeatures().back();
        if (bicycleSafety < bestBikeSafety) {
          bestBikeSafety = (float) bicycleSafety;
        }
        backStreet.setBicycleSafetyFactor((float) bicycleSafety);
        double walkSafety = wayData.getWalkSafetyFeatures().back();
        if (walkSafety < bestWalkSafety) {
          bestWalkSafety = (float) walkSafety;
        }
        backStreet.setWalkSafetyFactor((float) walkSafety);
        if (notes != null) {
          for (var it : notes) {
            graph.streetNotesService.addStaticNote(backStreet, it.note(), it.matcher());
          }
        }
        backStreet.setMotorVehicleNoThruTraffic(motorVehicleNoThrough);
        backStreet.setBicycleNoThruTraffic(bicycleNoThrough);
        backStreet.setWalkNoThruTraffic(walkNoThrough);
      }
    }

    // TODO Set this to private once WalkableAreaBuilder is gone
    protected I18NString getNameForWay(OSMWithTags way, String id) {
      I18NString name = way.getAssumedName();

      if (options.customNamer() != null && name != null) {
        name = new NonLocalizedString(options.customNamer().name(way, name.toString()));
      }

      if (name == null) {
        name = new NonLocalizedString(id);
      }
      return name;
    }

    /**
     * Make or get a shared vertex for flat intersections, or one vertex per level for multilevel
     * nodes like elevators. When there is an elevator or other Z-dimension discontinuity, a single
     * node can appear in several ways at different levels.
     *
     * @param node The node to fetch a label for.
     * @param way  The way it is connected to (for fetching level information).
     * @return vertex The graph vertex. This is not always an OSM vertex; it can also be a
     * {@link OsmBoardingLocationVertex}
     */
    protected IntersectionVertex getVertexForOsmNode(OSMNode node, OSMWithTags way) {
      // If the node should be decomposed to multiple levels,
      // use the numeric level because it is unique, the human level may not be (although
      // it will likely lead to some head-scratching if it is not).
      IntersectionVertex iv = null;
      if (node.isMultiLevel()) {
        // make a separate node for every level
        return recordLevel(node, way);
      }
      // single-level case
      long nid = node.getId();
      iv = intersectionNodes.get(nid);
      if (iv == null) {
        Coordinate coordinate = getCoordinate(node);
        String label = String.format(nodeLabelFormat, node.getId());
        String highway = node.getTag("highway");
        if ("motorway_junction".equals(highway)) {
          String ref = node.getTag("ref");
          if (ref != null) {
            ExitVertex ev = new ExitVertex(graph, label, coordinate.x, coordinate.y, nid);
            ev.setExitName(ref);
            iv = ev;
          }
        }

        /* If the OSM node represents a transit stop and has a ref=(stop_code) tag, make a special vertex for it. */
        if (node.isBoardingLocation()) {
          var refs = node.getMultiTagValues(options.boardingAreaRefTags());
          if (!refs.isEmpty()) {
            String name = node.getTag("name");
            iv =
              new OsmBoardingLocationVertex(
                graph,
                label,
                coordinate.x,
                coordinate.y,
                NonLocalizedString.ofNullable(name),
                refs
              );
          }
        }

        if (node.isBarrier()) {
          BarrierVertex bv = new BarrierVertex(graph, label, coordinate.x, coordinate.y, nid);
          bv.setBarrierPermissions(
            OsmFilter.getPermissionsForEntity(node, BarrierVertex.defaultBarrierPermissions)
          );
          iv = bv;
        }

        if (iv == null) {
          iv =
            new OsmVertex(
              graph,
              label,
              coordinate.x,
              coordinate.y,
              node.getId(),
              new NonLocalizedString(label),
              node.hasHighwayTrafficLight(),
              node.hasCrossingTrafficLight()
            );
        }

        intersectionNodes.put(nid, iv);
      }

      return iv;
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

    private List<AreaGroup> groupAreas(Collection<Area> areas) {
      Map<Area, OSMLevel> areasLevels = new HashMap<>(areas.size());
      for (Area area : areas) {
        areasLevels.put(area, osmdb.getLevelForWay(area.parent));
      }
      return AreaGroup.groupAreas(areasLevels);
    }

    private void buildWalkableAreas(boolean skipVisibility, boolean platformEntriesLinking) {
      if (skipVisibility) {
        LOG.info(
          "Skipping visibility graph construction for walkable areas and using just area rings for edges."
        );
      } else {
        LOG.info("Building visibility graphs for walkable areas.");
      }
      List<AreaGroup> areaGroups = groupAreas(osmdb.getWalkableAreas());
      WalkableAreaBuilder walkableAreaBuilder = new WalkableAreaBuilder(
        graph,
        osmdb,
        this,
        issueStore,
        options.maxAreaNodes(),
        options.platformEntriesLinking(),
        options.boardingAreaRefTags()
      );
      if (skipVisibility) {
        for (AreaGroup group : areaGroups) {
          walkableAreaBuilder.buildWithoutVisibility(group);
        }
      } else {
        ProgressTracker progress = ProgressTracker.track(
          "Build visibility graph for areas",
          50,
          areaGroups.size()
        );
        for (AreaGroup group : areaGroups) {
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

      WAY:for (OSMWay way : osmdb.getWays()) {
        WayProperties wayData = way.getOsmProvider().getWayPropertySet().getDataForWay(way);
        setWayName(way);
        StreetTraversalPermission permissions = OsmFilter.getPermissionsForWay(
          way,
          wayData.getPermission(),
          options.banDiscouragedWalking(),
          options.banDiscouragedBiking(),
          issueStore
        );
        if (!OsmFilter.isWayRoutable(way) || permissions.allowsNothing()) continue;

        // handle duplicate nodes in OSM ways
        // this is a workaround for crappy OSM data quality
        ArrayList<Long> nodes = new ArrayList<>(way.getNodeRefs().size());
        long last = -1;
        double lastLat = -1, lastLon = -1;
        String lastLevel = null;
        for (TLongIterator iter = way.getNodeRefs().iterator(); iter.hasNext();) {
          long nodeId = iter.next();
          OSMNode node = osmdb.getNode(nodeId);
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
        OSMNode osmStartNode = null;

        for (int i = 0; i < nodes.size() - 1; i++) {
          OSMNode segmentStartOSMNode = osmdb.getNode(nodes.get(i));

          if (segmentStartOSMNode == null) {
            continue;
          }

          Long endNode = nodes.get(i + 1);

          if (osmStartNode == null) {
            startNode = nodes.get(i);
            osmStartNode = segmentStartOSMNode;
          }
          // where the current edge might end
          OSMNode osmEndNode = osmdb.getNode(endNode);

          LineString geometry;

          /*
           * We split segments at intersections, self-intersections, nodes with ele tags, and transit stops;
           * the only processing we do on other nodes is to accumulate their geometry
           */
          if (segmentCoordinates.size() == 0) {
            segmentCoordinates.add(getCoordinate(osmStartNode));
          }

          if (
            intersectionNodes.containsKey(endNode) ||
            i == nodes.size() - 2 ||
            nodes.subList(0, i).contains(nodes.get(i)) ||
            osmEndNode.hasTag("ele") ||
            osmEndNode.isBoardingLocation() ||
            osmEndNode.isBarrier()
          ) {
            segmentCoordinates.add(getCoordinate(osmEndNode));

            geometry =
              GeometryUtils
                .getGeometryFactory()
                .createLineString(segmentCoordinates.toArray(new Coordinate[0]));
            segmentCoordinates.clear();
          } else {
            segmentCoordinates.add(getCoordinate(osmEndNode));
            continue;
          }

          /* generate endpoints */
          if (startEndpoint == null) { // first iteration on this way
            // make or get a shared vertex for flat intersections,
            // one vertex per level for multilevel nodes like elevators
            startEndpoint = getVertexForOsmNode(osmStartNode, way);
            String ele = segmentStartOSMNode.getTag("ele");
            if (ele != null) {
              Double elevation = ElevationUtils.parseEleTag(ele);
              if (elevation != null) {
                elevationData.put(startEndpoint, elevation);
              }
            }
          } else { // subsequent iterations
            startEndpoint = endEndpoint;
          }

          endEndpoint = getVertexForOsmNode(osmEndNode, way);
          String ele = osmEndNode.getTag("ele");
          if (ele != null) {
            Double elevation = ElevationUtils.parseEleTag(ele);
            if (elevation != null) {
              elevationData.put(endEndpoint, elevation);
            }
          }
          StreetEdgePair streets = getEdgesForStreet(
            startEndpoint,
            endEndpoint,
            way,
            i,
            permissions,
            geometry
          );

          StreetEdge street = streets.main;
          StreetEdge backStreet = streets.back;
          applyWayProperties(street, backStreet, wayData, way);

          applyEdgesToTurnRestrictions(way, startNode, endNode, street, backStreet);
          startNode = endNode;
          osmStartNode = osmdb.getNode(startNode);
        }

        //Keep lambda! A method-ref would log incorrect class and line number
        //noinspection Convert2MethodRef
        progress.step(m -> LOG.info(m));
      } // END loop over OSM ways

      LOG.info(progress.completeMessage());
    }

    private void setWayName(OSMWithTags way) {
      if (!way.hasTag("name")) {
        I18NString creativeName = way
          .getOsmProvider()
          .getWayPropertySet()
          .getCreativeNameForWay(way);
        if (creativeName != null) {
          //way.addTag("otp:gen_name", creativeName);
          way.setCreativeName(creativeName);
        }
      }
    }

    private void unifyTurnRestrictions() {
      // Note that usually when the from or to way is not found, it's because OTP has already
      // filtered that way. So many missing edges are not really problems worth issuing warnings on.
      for (Long fromWay : osmdb.getTurnRestrictionWayIds()) {
        for (TurnRestrictionTag restrictionTag : osmdb.getFromWayTurnRestrictions(fromWay)) {
          if (restrictionTag.possibleFrom.isEmpty()) {
            issueStore.add(
              new TurnRestrictionBad(restrictionTag.relationOSMID, "No from edge found")
            );
            continue;
          }
          if (restrictionTag.possibleTo.isEmpty()) {
            issueStore.add(
              new TurnRestrictionBad(restrictionTag.relationOSMID, "No to edge found")
            );
            continue;
          }
          for (StreetEdge from : restrictionTag.possibleFrom) {
            if (from == null) {
              issueStore.add(
                new TurnRestrictionBad(restrictionTag.relationOSMID, "from-edge is null")
              );
              continue;
            }
            for (StreetEdge to : restrictionTag.possibleTo) {
              if (to == null) {
                issueStore.add(
                  new TurnRestrictionBad(restrictionTag.relationOSMID, "to-edge is null")
                );
                continue;
              }
              int angleDiff = from.getOutAngle() - to.getInAngle();
              if (angleDiff < 0) {
                angleDiff += 360;
              }
              switch (restrictionTag.direction) {
                case LEFT:
                  if (angleDiff >= 160) {
                    issueStore.add(
                      new TurnRestrictionBad(
                        restrictionTag.relationOSMID,
                        "Left turn restriction is not on edges which turn left"
                      )
                    );
                    continue; // not a left turn
                  }
                  break;
                case RIGHT:
                  if (angleDiff <= 200) {
                    issueStore.add(
                      new TurnRestrictionBad(
                        restrictionTag.relationOSMID,
                        "Right turn restriction is not on edges which turn right"
                      )
                    );
                    continue; // not a right turn
                  }
                  break;
                case U:
                  if ((angleDiff <= 150 || angleDiff > 210)) {
                    issueStore.add(
                      new TurnRestrictionBad(
                        restrictionTag.relationOSMID,
                        "U-turn restriction is not on U-turn"
                      )
                    );
                    continue; // not a U turn
                  }
                  break;
                case STRAIGHT:
                  if (angleDiff >= 30 && angleDiff < 330) {
                    issueStore.add(
                      new TurnRestrictionBad(
                        restrictionTag.relationOSMID,
                        "Straight turn restriction is not on edges which go straight"
                      )
                    );
                    continue; // not straight
                  }
                  break;
              }
              TurnRestriction restriction = new TurnRestriction(
                from,
                to,
                restrictionTag.type,
                restrictionTag.modes,
                restrictionTag.time
              );
              from.addTurnRestriction(restriction);
            }
          }
        }
      }
    }

    private void applyEdgesToTurnRestrictions(
      OSMWay way,
      long startNode,
      long endNode,
      StreetEdge street,
      StreetEdge backStreet
    ) {
      /* Check if there are turn restrictions starting on this segment */
      Collection<TurnRestrictionTag> restrictionTags = osmdb.getFromWayTurnRestrictions(
        way.getId()
      );

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

    private void initIntersectionNodes() {
      Set<Long> possibleIntersectionNodes = new HashSet<>();
      for (OSMWay way : osmdb.getWays()) {
        TLongList nodes = way.getNodeRefs();
        nodes.forEach(node -> {
          if (possibleIntersectionNodes.contains(node)) {
            intersectionNodes.put(node, null);
          } else {
            possibleIntersectionNodes.add(node);
          }
          return true;
        });
      }
      // Intersect ways at area boundaries if needed.
      for (Area area : Iterables.concat(
        osmdb.getWalkableAreas(),
        osmdb.getParkAndRideAreas(),
        osmdb.getBikeParkingAreas()
      )) {
        for (Ring outerRing : area.outermostRings) {
          intersectAreaRingNodes(possibleIntersectionNodes, outerRing);
        }
      }
    }

    private void intersectAreaRingNodes(Set<Long> possibleIntersectionNodes, Ring outerRing) {
      for (OSMNode node : outerRing.nodes) {
        long nodeId = node.getId();
        if (possibleIntersectionNodes.contains(nodeId)) {
          intersectionNodes.put(nodeId, null);
        } else {
          possibleIntersectionNodes.add(nodeId);
        }
      }

      outerRing.getHoles().forEach(hole -> intersectAreaRingNodes(possibleIntersectionNodes, hole));
    }

    /**
     * The safest bike lane should have a safety weight no lower than the time weight of a flat
     * street. This method divides the safety lengths by the length ratio of the safest street,
     * ensuring this property.
     * <p>
     * TODO Move this away, this is common to all street builders.
     */
    private void applySafetyFactors(Graph graph) {
      issueStore.add(
        new Graphwide("Multiplying all bike safety values by " + (1 / bestBikeSafety))
      );
      issueStore.add(
        new Graphwide("Multiplying all walk safety values by " + (1 / bestWalkSafety))
      );
      HashSet<Edge> seenEdges = new HashSet<>();
      HashSet<AreaEdgeList> seenAreas = new HashSet<>();
      for (Vertex vertex : graph.getVertices()) {
        for (Edge e : vertex.getOutgoing()) {
          if (e instanceof AreaEdge) {
            AreaEdgeList areaEdgeList = ((AreaEdge) e).getArea();
            if (seenAreas.contains(areaEdgeList)) continue;
            seenAreas.add(areaEdgeList);
            for (NamedArea area : areaEdgeList.getAreas()) {
              area.setBicycleSafetyMultiplier(area.getBicycleSafetyMultiplier() / bestBikeSafety);
              area.setWalkSafetyMultiplier(area.getWalkSafetyMultiplier() / bestWalkSafety);
            }
          }
          if (!(e instanceof StreetEdge)) {
            continue;
          }
          StreetEdge pse = (StreetEdge) e;

          if (!seenEdges.contains(e)) {
            seenEdges.add(e);
            pse.setBicycleSafetyFactor(pse.getBicycleSafetyFactor() / bestBikeSafety);
            pse.setWalkSafetyFactor(pse.getWalkSafetyFactor() / bestWalkSafety);
          }
        }
        for (Edge e : vertex.getIncoming()) {
          if (!(e instanceof StreetEdge)) {
            continue;
          }
          StreetEdge pse = (StreetEdge) e;

          if (!seenEdges.contains(e)) {
            seenEdges.add(e);
            pse.setBicycleSafetyFactor(pse.getBicycleSafetyFactor() / bestBikeSafety);
            pse.setWalkSafetyFactor(pse.getWalkSafetyFactor() / bestWalkSafety);
          }
        }
      }
    }

    private Coordinate getCoordinate(OSMNode osmNode) {
      return new Coordinate(osmNode.lon, osmNode.lat);
    }

    /**
     * Handle oneway streets, cycleways, and other per-mode and universal access controls. See
     * http://wiki.openstreetmap.org/wiki/Bicycle for various scenarios, along with
     * http://wiki.openstreetmap.org/wiki/OSM_tags_for_routing#Oneway.
     */
    private StreetEdgePair getEdgesForStreet(
      IntersectionVertex startEndpoint,
      IntersectionVertex endEndpoint,
      OSMWay way,
      int index,
      StreetTraversalPermission permissions,
      LineString geometry
    ) {
      // No point in returning edges that can't be traversed by anyone.
      if (permissions.allowsNothing()) {
        return new StreetEdgePair(null, null);
      }

      LineString backGeometry = geometry.reverse();
      StreetEdge street = null, backStreet = null;
      double length = getGeometryLengthMeters(geometry);

      var permissionPair = OsmFilter.getPermissions(permissions, way);
      var permissionsFront = permissionPair.main();
      var permissionsBack = permissionPair.back();

      if (permissionsFront.allowsAnything()) {
        street =
          getEdgeForStreet(
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
        backStreet =
          getEdgeForStreet(
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

      /* mark edges that are on roundabouts */
      if (way.isRoundabout()) {
        if (street != null) street.setRoundabout(true);
        if (backStreet != null) backStreet.setRoundabout(true);
      }

      return new StreetEdgePair(street, backStreet);
    }

    private StreetEdge getEdgeForStreet(
      IntersectionVertex startEndpoint,
      IntersectionVertex endEndpoint,
      OSMWay way,
      int index,
      double length,
      StreetTraversalPermission permissions,
      LineString geometry,
      boolean back
    ) {
      String label = "way " + way.getId() + " from " + index;
      label = label.intern();
      I18NString name = getNameForWay(way, label);
      float carSpeed = way.getOsmProvider().getOsmTagMapper().getCarSpeedForWay(way, back);

      StreetEdge street = new StreetEdge(
        startEndpoint,
        endEndpoint,
        geometry,
        name,
        length,
        permissions,
        back
      );
      street.setCarSpeed(carSpeed);
      street.setLink(OsmFilter.isLink(way));

      if (!way.hasTag("name") && !way.hasTag("ref")) {
        street.setHasBogusName(true);
      }

      boolean steps = way.isSteps();
      street.setStairs(steps);

      /* TODO: This should probably generalized somehow? */
      if ((way.isTagFalse("wheelchair") || (steps && !way.isTagTrue("wheelchair")))) {
        street.setWheelchairAccessible(false);
      }

      street.setSlopeOverride(way.getOsmProvider().getWayPropertySet().getSlopeOverride(way));

      // < 0.04: account for
      if (carSpeed < 0.04) {
        issueStore.add(new StreetCarSpeedZero(way));
      }

      if (options.customNamer() != null) {
        options.customNamer().nameWithEdge(way, street);
      }

      return street;
    }

    /**
     * Record the level of the way for this node, e.g. if the way is at level 5, mark that this node
     * is active at level 5.
     *
     * @param way  the way that has the level
     * @param node the node to record for
     * @author mattwigway
     */
    private OsmVertex recordLevel(OSMNode node, OSMWithTags way) {
      OSMLevel level = osmdb.getLevelForWay(way);
      Map<OSMLevel, OsmVertex> vertices;
      long nodeId = node.getId();
      if (multiLevelNodes.containsKey(nodeId)) {
        vertices = multiLevelNodes.get(nodeId);
      } else {
        vertices = new HashMap<>();
        multiLevelNodes.put(nodeId, vertices);
      }
      if (!vertices.containsKey(level)) {
        Coordinate coordinate = getCoordinate(node);
        String label = String.format(levelnodeLabelFormat, node.getId(), level.shortName);
        OsmVertex vertex = new OsmVertex(
          graph,
          label,
          coordinate.x,
          coordinate.y,
          node.getId(),
          new NonLocalizedString(label),
          false,
          false
        );
        vertices.put(level, vertex);

        return vertex;
      }
      return vertices.get(level);
    }
  }
}
