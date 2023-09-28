package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.Iterables;
import gnu.trove.list.TLongList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.openstreetmap.model.OSMLevel;
import org.opentripplanner.openstreetmap.model.OSMNode;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.ElevatorEdge;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.OsmVertex;
import org.opentripplanner.street.model.vertex.VertexFactory;

/**
 * Tracks the generation of vertices and returns an existing instance if a vertex is encountered
 * more than once.
 */
class VertexGenerator {

  private static final String nodeLabelFormat = "osm:node:%d";

  private final Map<Long, IntersectionVertex> intersectionNodes = new HashMap<>();

  private final HashMap<Long, Map<OSMLevel, OsmVertex>> multiLevelNodes = new HashMap<>();
  private final OsmDatabase osmdb;
  private final Set<String> boardingAreaRefTags;
  private final VertexFactory vertexFactory;

  public VertexGenerator(OsmDatabase osmdb, Graph graph, Set<String> boardingAreaRefTags) {
    this.osmdb = osmdb;
    this.vertexFactory = new VertexFactory(graph);
    this.boardingAreaRefTags = boardingAreaRefTags;
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
  IntersectionVertex getVertexForOsmNode(OSMNode node, OSMWithTags way) {
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
          iv =
            vertexFactory.osmBoardingLocation(
              coordinate,
              label,
              refs,
              NonLocalizedString.ofNullable(name)
            );
        }
      }

      if (node.isBarrier()) {
        BarrierVertex bv = vertexFactory.barrier(nid, coordinate);
        bv.setBarrierPermissions(node.overridePermissions(BarrierVertex.defaultBarrierPermissions));
        iv = bv;
      }

      if (iv == null) {
        iv =
          vertexFactory.osm(
            coordinate,
            node,
            node.hasHighwayTrafficLight(),
            node.hasCrossingTrafficLight()
          );
      }

      intersectionNodes.put(nid, iv);
    }

    return iv;
  }

  /**
   * Tracks OSM nodes which are decomposed into multiple graph vertices because they are
   * elevators. They can then be iterated over to build {@link ElevatorEdge} between them.
   */
  Map<Long, Map<OSMLevel, OsmVertex>> multiLevelNodes() {
    return multiLevelNodes;
  }

  void initIntersectionNodes() {
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
      OsmVertex vertex = vertexFactory.levelledOsm(node, level.shortName);
      vertices.put(level, vertex);

      return vertex;
    }
    return vertices.get(level);
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
}
