package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.LinearBarrierNodeType.SPLIT;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import gnu.trove.list.TLongList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.BarrierIntersectingHighway;
import org.opentripplanner.graph_builder.issues.DifferentLevelsSharingBarrier;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorEdge;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.VertexFactory;
import org.opentripplanner.transit.model.basic.Accessibility;

/**
 * Tracks the generation of vertices and returns an existing instance if a vertex is encountered
 * more than once.
 */
class VertexGenerator {

  private static final String nodeLabelFormat = "osm:node:%d";

  private final Map<Long, IntersectionVertex> intersectionNodes = new HashMap<>();

  private final HashMap<Long, Map<OsmLevel, OsmVertex>> multiLevelNodes = new HashMap<>();

  /**
   * The map from node to the barrier it belongs to.
   */
  private final Multimap<OsmNode, OsmWay> nodesInBarrierWays = HashMultimap.create();

  /**
   * The levels of the ways connecting barrier nodes. Used for issue reporting only.
   */
  private final Multimap<OsmNode, OsmLevel> levelsConnectingBarriers = HashMultimap.create();
  private final Set<OsmNode> reportedLevelBarrierNodes = new HashSet<>();
  private final Set<OsmNode> reportedLinearBarrierCrossings = new HashSet<>();

  /**
   * The map from node to the split vertex for each routable way connecting it.
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
   * node can appear in several ways at different levels.
   *
   * @param node The node to fetch a label for.
   * @param way  The way it is connected to (for fetching level information).
   * @param linearBarrierNodeType How should the node be handled if it is on a linear barrier
   * @return vertex The graph vertex. This is not always an OSM vertex; it can also be a
   * {@link OsmBoardingLocationVertex}
   */
  IntersectionVertex getVertexForOsmNode(
    OsmNode node,
    OsmEntity way,
    LinearBarrierNodeType linearBarrierNodeType
  ) {
    // If the node should be decomposed to multiple levels,
    // use the numeric level because it is unique, the human level may not be (although
    // it will likely lead to some head-scratching if it is not).
    IntersectionVertex iv = null;
    if (node.isMultiLevel()) {
      // make a separate node for every level
      return recordLevel(node, way);
    }
    // make a separate vertex if the node is on a barrier
    boolean isNodeOnLinearBarrier = nodesInBarrierWays.containsKey(node);
    if (linearBarrierNodeType == SPLIT && isNodeOnLinearBarrier) {
      return getSplitVertexOnBarrier(node, way);
    }
    // single-level case
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
        String label = String.format(nodeLabelFormat, node.getId());
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
        bv.setBarrierPermissions(node.overridePermissions(BarrierVertex.defaultBarrierPermissions));
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
      checkLevelOnBarrier(node, way);
    }

    return iv;
  }

  /**
   * If a node is on a barrier, a vertex needs to be created for each way using it.
   *
   * @return a vertex for the given node specific to the given way
   */
  private IntersectionVertex getSplitVertexOnBarrier(OsmNode nodeOnBarrier, OsmEntity way) {
    checkLevelOnBarrier(nodeOnBarrier, way);

    splitVerticesOnBarriers.putIfAbsent(nodeOnBarrier, new HashMap<>());
    var vertices = splitVerticesOnBarriers.get(nodeOnBarrier);
    var existing = vertices.get(way);
    if (existing != null) {
      return existing;
    }

    var vertex = vertexFactory.osmOnLinearBarrier(
      nodeOnBarrier.getCoordinate(),
      nodeOnBarrier.getId(),
      way.getId()
    );
    vertices.put(way, vertex);
    return vertex;
  }

  private void checkLevelOnBarrier(OsmNode nodeOnBarrier, OsmEntity way) {
    var level = osmdb.getLevelForWay(way);
    if (!reportedLevelBarrierNodes.contains(nodeOnBarrier)) {
      var existingLevels = levelsConnectingBarriers.get(nodeOnBarrier);
      if (existingLevels.stream().anyMatch(l -> !Objects.requireNonNull(l).equals(level))) {
        issueStore.add(new DifferentLevelsSharingBarrier(nodeOnBarrier));
        reportedLevelBarrierNodes.add(nodeOnBarrier);
      }
    }
    levelsConnectingBarriers.put(nodeOnBarrier, level);
  }

  /**
   * Tracks OSM nodes which are decomposed into multiple graph vertices because they are
   * elevators. They can then be iterated over to build {@link ElevatorEdge} between them.
   */
  Map<Long, Map<OsmLevel, OsmVertex>> multiLevelNodes() {
    return multiLevelNodes;
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
   * Record the level of the way for this node, e.g. if the way is at level 5, mark that this node
   * is active at level 5.
   *
   * @param way  the way that has the level
   * @param node the node to record for
   * @author mattwigway
   */
  private OsmVertex recordLevel(OsmNode node, OsmEntity way) {
    OsmLevel level = osmdb.getLevelForWay(way);
    Map<OsmLevel, OsmVertex> vertices;
    long nodeId = node.getId();
    if (multiLevelNodes.containsKey(nodeId)) {
      vertices = multiLevelNodes.get(nodeId);
    } else {
      vertices = new HashMap<>();
      multiLevelNodes.put(nodeId, vertices);
    }
    if (!vertices.containsKey(level)) {
      OsmVertex vertex = vertexFactory.levelledOsm(node, level.shortName);
      vertices.put(level, vertex);

      return vertex;
    }
    return vertices.get(level);
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
}
