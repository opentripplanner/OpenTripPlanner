package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.SPLIT;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import gnu.trove.list.TLongList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BarrierIntersectingHighway;
import org.opentripplanner.graph_builder.issues.DifferentLevelsSharingBarrier;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorEdge;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.OsmElevatorVertex;
import org.opentripplanner.street.model.vertex.OsmEntityType;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.streetadapter.VertexFactory;

/**
 * Tracks the generation of vertices and returns an existing instance if a vertex is encountered
 * more than once.
 */
class VertexGenerator {

  private static final String NODE_LABEL_FORMAT = "osm:node:%d";

  private final Map<Long, IntersectionVertex> intersectionNodes = new HashMap<>();

  private final HashMap<Long, Map<OsmElevatorKey, OsmElevatorVertex>> elevatorNodes =
    new HashMap<>();
  private final HashMap<OsmElevatorKey, OsmLevel> elevatorNodeLevels = new HashMap<>();

  /**
   * The map from node to the barrier it belongs to.
   */
  private final Multimap<OsmNode, OsmWay> nodesInBarrierWays = HashMultimap.create();

  /**
   * The levels of the entities connecting barrier nodes. Used for issue reporting only.
   */
  private final Multimap<OsmNode, OsmLevel> levelsConnectingBarriers = HashMultimap.create();
  private final Set<OsmNode> reportedLinearBarrierCrossings = new HashSet<>();

  /**
   * The map from node to the split vertex for each routable entity connecting it.
   */
  private final Map<OsmNode, Map<OsmEntity, OsmVertex>> splitVerticesOnBarriers = new HashMap<>();
  private final OsmDatabase osmdb;
  private final Set<String> boardingAreaRefTags;
  private final Boolean includeOsmSubwayEntrances;
  private final VertexFactory vertexFactory;
  private final DataImportIssueStore issueStore;

  public VertexGenerator(
    OsmDatabase osmdb,
    Graph graph,
    Set<String> boardingAreaRefTags,
    boolean includeOsmSubwayEntrances,
    DataImportIssueStore issueStore
  ) {
    this.osmdb = osmdb;
    this.vertexFactory = new VertexFactory(graph);
    this.boardingAreaRefTags = boardingAreaRefTags;
    this.includeOsmSubwayEntrances = includeOsmSubwayEntrances;
    this.issueStore = issueStore;
  }

  /**
   * Make or get a shared vertex for flat intersections, or one vertex per level for multilevel
   * nodes like elevators. When there is an elevator or other Z-dimension discontinuity, a single
   * node can appear in several entities at different levels.
   *
   * @param node The node to fetch a label for.
   * @param entity The entity it is connected to, e.g. used for fetching level information.
   * @param linearBarrierNodeType How should the node be handled if it is on a linear barrier
   *
   * @return The graph vertex. This is not always an OSM vertex; it can also be a
   * {@link OsmBoardingLocationVertex}
   */
  IntersectionVertex getVertexForOsmNode(
    OsmNode node,
    OsmEntity entity,
    LinearBarrierNodeType linearBarrierNodeType
  ) {
    if (node.isElevator()) {
      return getElevatorVertex(node, entity);
    }
    boolean isNodeOnLinearBarrier = nodesInBarrierWays.containsKey(node);
    if (linearBarrierNodeType == SPLIT && isNodeOnLinearBarrier) {
      return getSplitVertexOnBarrier(node, entity);
    }
    // single-level case
    IntersectionVertex iv = null;
    long nid = node.getId();
    iv = intersectionNodes.get(nid);
    if (iv == null) {
      Coordinate coordinate = node.getCoordinate();
      String highway = node.getTag("highway");
      if ("motorway_junction".equals(highway)) {
        String ref = node.getTag("ref");
        if (ref != null) {
          iv = vertexFactory.exit(nid, coordinate, ref);
        }
      }

      /* If the OSM node represents a transit stop and has a ref=(stop_code) tag, make a special vertex for it. */
      if (node.isBoardingLocation()) {
        String label = String.format(NODE_LABEL_FORMAT, node.getId());
        var refs = node.getMultiTagValues(boardingAreaRefTags);
        if (!refs.isEmpty()) {
          String name = node.getTag("name");
          iv = vertexFactory.osmBoardingLocation(
            coordinate,
            label,
            refs,
            NonLocalizedString.ofNullable(name)
          );
        }
      }

      if (includeOsmSubwayEntrances && node.isSubwayEntrance()) {
        String ref = node.getTag("ref");
        iv = vertexFactory.stationEntrance(
          nid,
          coordinate,
          ref,
          node.explicitWheelchairAccessibility()
        );
      }

      if (iv == null && node.isBarrier()) {
        iv = vertexFactory.barrier(nid, coordinate, node.explicitWheelchairAccessibility());
      }

      if (iv instanceof BarrierVertex bv) {
        bv.setBarrierPermissions(
          node.overridePermissions(BarrierVertex.DEFAULT_BARRIER_PERMISSIONS)
        );
        if (
          bv.wheelchairAccessibility() == Accessibility.NO_INFORMATION &&
          !node.isWheelchairAccessible()
        ) {
          bv.setWheelchairAccessibility(Accessibility.NOT_POSSIBLE);
        }
      }

      if (iv == null) {
        iv = vertexFactory.osm(
          coordinate,
          node.getId(),
          node.hasHighwayTrafficLight(),
          node.hasCrossingTrafficLight()
        );
      }

      if (isNodeOnLinearBarrier && iv instanceof OsmVertex ov) {
        splitVerticesOnBarriers.putIfAbsent(node, new HashMap<>());
        var vertices = splitVerticesOnBarriers.get(node);
        vertices.put(null, ov);

        if (!node.isTaggedBarrierCrossing() && !reportedLinearBarrierCrossings.contains(node)) {
          issueStore.add(new BarrierIntersectingHighway(node));
          reportedLinearBarrierCrossings.add(node);
        }
      }

      intersectionNodes.put(nid, iv);
    }

    if (iv instanceof BarrierVertex) {
      saveBarrierLevels(node, entity);
    }

    return iv;
  }

  /**
   * If a node is on a barrier, a separate vertex needs to be created for each entity using it.
   *
   * @return a vertex for the given node specific to the given entity
   */
  private IntersectionVertex getSplitVertexOnBarrier(OsmNode nodeOnBarrier, OsmEntity entity) {
    saveBarrierLevels(nodeOnBarrier, entity);

    splitVerticesOnBarriers.putIfAbsent(nodeOnBarrier, new HashMap<>());
    var vertices = splitVerticesOnBarriers.get(nodeOnBarrier);
    var existing = vertices.get(entity);
    if (existing != null) {
      return existing;
    }

    var vertex = vertexFactory.osmOnLinearBarrier(
      nodeOnBarrier.getCoordinate(),
      nodeOnBarrier.getId(),
      getOsmEntityType(entity),
      entity.getId()
    );
    vertices.put(entity, vertex);
    return vertex;
  }

  private void saveBarrierLevels(OsmNode nodeOnBarrier, OsmEntity entity) {
    levelsConnectingBarriers.putAll(nodeOnBarrier, getLevelsForNode(nodeOnBarrier, entity));
  }

  public Multimap<OsmNode, OsmLevel> getLevelsConnectingBarriers() {
    return this.levelsConnectingBarriers;
  }

  private List<OsmLevel> getLevelsForNode(OsmNode node, OsmEntity entity) {
    List<OsmLevel> levels = osmdb.getLevelsForEntity(entity);
    if (entity instanceof OsmWay way && way.isSteps()) {
      if (levels.size() == 2) {
        var nodeRefs = way.getNodeRefs();
        long firstNodeRef = nodeRefs.get(0);
        long lastNodeRef = nodeRefs.get(nodeRefs.size() - 1);
        if (node.getId() == firstNodeRef) {
          return List.of(levels.getFirst());
        } else if (node.getId() == lastNodeRef) {
          return List.of(levels.getLast());
        }
      }
      return List.of();
    }
    return levels;
  }

  /**
   * Tracks OSM nodes which are decomposed into multiple graph vertices because they are
   * elevators. They can then be iterated over to build {@link ElevatorEdge} between them.
   */
  Map<Long, Map<OsmElevatorKey, OsmElevatorVertex>> elevatorNodes() {
    return elevatorNodes;
  }

  HashMap<OsmElevatorKey, OsmLevel> elevatorNodeLevels() {
    return elevatorNodeLevels;
  }

  void initIntersectionNodes() {
    Set<Long> possibleIntersectionNodes = new HashSet<>();
    for (OsmWay way : osmdb.getWays()) {
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
    for (OsmArea area : Iterables.concat(
      osmdb.getWalkableAreas(),
      osmdb.getParkAndRideAreas(),
      osmdb.getBikeParkingAreas()
    )) {
      for (Ring outerRing : area.outermostRings) {
        intersectAreaRingNodes(possibleIntersectionNodes, outerRing);
      }
    }
  }

  Collection<OsmWay> getLinearBarriersAtNode(OsmNode node) {
    return nodesInBarrierWays.get(node);
  }

  /**
   * Get a mapping from a node to a map of vertices for that node, indexed by the applicable area
   * the vertex is in. The null-indexed vertex, if exists, is the vertex which hasn't been split
   * for a particular area and is applicable for all linear crossing of the barrier.
   */
  Map<OsmNode, Map<OsmEntity, OsmVertex>> splitVerticesOnBarriers() {
    return splitVerticesOnBarriers;
  }

  public Multimap<OsmNode, OsmWay> nodesInBarrierWays() {
    return nodesInBarrierWays;
  }

  void initNodesInBarrierWays() {
    for (OsmWay way : osmdb.getWays()) {
      if (way.isBarrier()) {
        TLongList nodes = way.getNodeRefs();
        boolean isClosed = nodes.get(0) == nodes.get(nodes.size() - 1);
        for (int i = 0; i < nodes.size() - (isClosed ? 1 : 0); i++) {
          OsmNode node = osmdb.getNode(nodes.get(i));
          if (node != null) {
            nodesInBarrierWays.put(node, way);
          }
        }
      }
    }
  }

  /**
   * Track OSM nodes that will become graph vertices because they appear in multiple OSM ways
   */
  Map<Long, IntersectionVertex> intersectionNodes() {
    return intersectionNodes;
  }

  /**
   * Create a separate elevator vertex for each connected entity and record its level from
   * the entity's level.
   * <p>
   * If the entity is at level 5, mark that this node is active at level 5. If another entity
   * connected to the node is also at level 5, create a separate vertex for that combination, but
   * mark the level as the same.
   *
   * @param entity an entity on a level
   * @param node the node to create the elevator vertex from
   */
  private OsmElevatorVertex getElevatorVertex(OsmNode node, OsmEntity entity) {
    Map<OsmElevatorKey, OsmElevatorVertex> elevatorVertices = getElevatorVertices(node);
    OsmEntityType osmEntityType = getOsmEntityType(entity);
    OsmElevatorKey osmElevatorKey = new OsmElevatorKey(node.getId(), osmEntityType, entity.getId());
    if (!elevatorVertices.containsKey(osmElevatorKey)) {
      OsmElevatorVertex vertex = vertexFactory.osmElevator(node, osmEntityType, entity.getId());
      elevatorVertices.put(osmElevatorKey, vertex);
      // An OsmElevatorVertex requires one level to be defined.
      elevatorNodeLevels.put(osmElevatorKey, osmdb.findSingleLevelForEntity(entity));
      return vertex;
    }
    return elevatorVertices.get(osmElevatorKey);
  }

  private Map<OsmElevatorKey, OsmElevatorVertex> getElevatorVertices(OsmNode node) {
    long nodeId = node.getId();
    if (elevatorNodes.containsKey(nodeId)) {
      return elevatorNodes.get(nodeId);
    }
    Map<OsmElevatorKey, OsmElevatorVertex> elevatorVertices = new HashMap<>();
    elevatorNodes.put(nodeId, elevatorVertices);
    return elevatorVertices;
  }

  private void intersectAreaRingNodes(Set<Long> possibleIntersectionNodes, Ring outerRing) {
    for (OsmNode node : outerRing.nodes) {
      long nodeId = node.getId();
      if (possibleIntersectionNodes.contains(nodeId)) {
        intersectionNodes.put(nodeId, null);
      } else {
        possibleIntersectionNodes.add(nodeId);
      }
    }

    outerRing.getHoles().forEach(hole -> intersectAreaRingNodes(possibleIntersectionNodes, hole));
  }

  public void createDifferentLevelsSharingBarrierIssues() {
    Multimap<OsmNode, OsmLevel> levelsConnectingBarriers = getLevelsConnectingBarriers();
    for (OsmNode nodeOnBarrier : levelsConnectingBarriers.keySet()) {
      var levels = levelsConnectingBarriers.get(nodeOnBarrier);
      if (levels.size() > 1) {
        issueStore.add(new DifferentLevelsSharingBarrier(nodeOnBarrier, levels.size()));
      }
    }
  }

  private OsmEntityType getOsmEntityType(OsmEntity entity) {
    return entity instanceof OsmWay ? OsmEntityType.WAY : OsmEntityType.RELATION;
  }
}
