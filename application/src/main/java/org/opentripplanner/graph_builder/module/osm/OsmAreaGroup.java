package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.osm.Ring.RingConstructionException;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A group of possibly-contiguous areas sharing the same level
 */
class OsmAreaGroup {

  private static final Logger LOG = LoggerFactory.getLogger(OsmAreaGroup.class);

  /*
   * The list of underlying areas, used when generating edges out of the visibility graph
   */
  Collection<OsmArea> areas;

  /**
   * The joined outermost rings of the areas (with inner rings for holes as necessary).
   */
  List<Ring> outermostRings = new ArrayList<>();

  public final Geometry union;

  public OsmAreaGroup(Collection<OsmArea> areas) {
    this.areas = areas;

    // Merging non-convex polygons is complicated, so we need to convert to JTS, let JTS do the
    // hard work,
    // then convert back.
    List<Polygon> allRings = new ArrayList<>();

    // However, JTS will lose the coord<->osmnode mapping, and we will have to reconstruct it.
    HashMap<Coordinate, OsmNode> nodeMap = new HashMap<>();
    for (OsmArea area : areas) {
      for (Ring ring : area.outermostRings) {
        allRings.add(ring.jtsPolygon);
        for (OsmNode node : ring.nodes) {
          nodeMap.put(new Coordinate(node.lon, node.lat), node);
        }
        for (Ring inner : ring.getHoles()) {
          for (OsmNode node : inner.nodes) {
            nodeMap.put(new Coordinate(node.lon, node.lat), node);
          }
        }
      }
    }
    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    Geometry allPolygons = geometryFactory.createMultiPolygon(allRings.toArray(new Polygon[0]));
    this.union = allPolygons.union();

    if (this.union instanceof GeometryCollection coll) {
      GeometryCollection mp = coll;
      for (int i = 0; i < mp.getNumGeometries(); ++i) {
        Geometry geom = mp.getGeometryN(i);
        if (geom instanceof Polygon polygon) {
          outermostRings.add(toRing(polygon, nodeMap));
        } else {
          LOG.warn("Unexpected non-polygon when merging areas: {}", geom);
        }
      }
    } else if (this.union instanceof Polygon polygon) {
      outermostRings.add(toRing(polygon, nodeMap));
    } else {
      LOG.warn("Unexpected non-polygon when merging areas: {}", this.union);
    }
  }

  /**
   * @return barriers which has a direct edge from first to second
   */
  private static Collection<OsmWay> getBarriers(
    Multimap<OsmNode, OsmWay> barriers,
    OsmNode first,
    OsmNode second
  ) {
    return CollectionUtils.intersection(barriers.get(first), barriers.get(second))
      .stream()
      .filter(
        // only return barriers where first and second are consecutive nodes
        barrier -> {
          var nodeRefs = Objects.requireNonNull(barrier).getNodeRefs();
          for (var i = 0; i < nodeRefs.size() - 1; ++i) {
            if (nodeRefs.get(i) == first.getId() && nodeRefs.get(i + 1) == second.getId()) {
              return true;
            }
            if (nodeRefs.get(i) == second.getId() && nodeRefs.get(i + 1) == first.getId()) {
              return true;
            }
          }
          return false;
        }
      )
      .toList();
  }

  public static List<OsmAreaGroup> groupAreas(
    Map<OsmArea, OsmLevel> areasLevels,
    Multimap<OsmNode, OsmWay> barriers
  ) {
    DisjointSet<OsmArea> groups = new DisjointSet<>();
    Multimap<OsmNodePair, OsmArea> areasForNodePair = HashMultimap.create();
    Map<OsmArea, Map<OsmWay, Set<OsmNodePair>>> barriersForArea = new HashMap<>();
    for (OsmArea area : areasLevels.keySet()) {
      for (Ring ring : area.outermostRings) {
        for (Ring inner : ring.getHoles()) {
          processRing(area, inner, barriers, areasForNodePair, barriersForArea);
        }
        processRing(area, ring, barriers, areasForNodePair, barriersForArea);
      }
    }

    // areas that can be joined must share levels and also at least two consecutive nodes,
    // and these two consecutive nodes must not also be consecutive nodes on a barrier
    for (var nodePair : areasForNodePair.keySet()) {
      for (OsmArea area1 : areasForNodePair.get(nodePair)) {
        OsmLevel level1 = areasLevels.get(area1);
        for (OsmArea area2 : areasForNodePair.get(nodePair)) {
          OsmLevel level2 = areasLevels.get(area2);
          boolean onSameLevel =
            (level1 == null && level2 == null) || (level1 != null && level1.equals(level2));
          var crossablePermissions = Objects.requireNonNull(area1)
            .getPermission()
            .intersection(Objects.requireNonNull(area2).getPermission());
          Collection<OsmWay> sharedBarriers = CollectionUtils.intersection(
            barriersForArea.getOrDefault(area1, Map.of()).keySet(),
            barriersForArea.getOrDefault(area2, Map.of()).keySet()
          )
            .stream()
            .filter(barrier -> {
              boolean blocksTraversal =
                crossablePermissions.intersection(
                  Objects.requireNonNull(barrier).getPermission()
                ) !=
                crossablePermissions;
              boolean sharesEdgeWithBothAreas = !CollectionUtils.intersection(
                barriersForArea.get(area1).get(barrier),
                barriersForArea.get(area2).get(barrier)
              ).isEmpty();
              return blocksTraversal && sharesEdgeWithBothAreas;
            })
            .toList();
          boolean shareBarrier = area1 != area2 && !sharedBarriers.isEmpty();
          if (onSameLevel && !shareBarrier) {
            groups.union(area1, area2);
          }
        }
      }
    }

    List<OsmAreaGroup> out = new ArrayList<>();
    for (Set<OsmArea> areaSet : groups.sets()) {
      try {
        out.add(new OsmAreaGroup(areaSet));
      } catch (RingConstructionException e) {
        for (OsmArea area : areaSet) {
          LOG.debug(
            "Failed to create merged area for " +
            area +
            ".  This area might not be at fault; it might be one of the other areas in this list."
          );
          out.add(new OsmAreaGroup(Arrays.asList(area)));
        }
      }
    }
    return out;
  }

  private static void processRing(
    OsmArea area,
    Ring ring,
    Multimap<OsmNode, OsmWay> barriers,
    Multimap<OsmNodePair, OsmArea> areasForNodePair,
    Map<OsmArea, Map<OsmWay, Set<OsmNodePair>>> barriersForArea
  ) {
    var nodes = ring.nodes;
    // the end node of a ring must be the same of the start node
    for (var i = 0; i < nodes.size() - 1; i++) {
      OsmNode node = nodes.get(i);
      OsmNode nextNode = nodes.get(i + 1);
      var pair = new OsmNodePair(node, nextNode);
      areasForNodePair.put(pair, area);

      var sharedBarriers = getBarriers(barriers, node, nextNode);
      for (var barrier : sharedBarriers) {
        if (!barriersForArea.containsKey(area)) {
          barriersForArea.put(area, new HashMap<>());
        }
        var barrierMap = barriersForArea.get(area);
        if (!barrierMap.containsKey(barrier)) {
          barrierMap.put(barrier, new HashSet<>());
        }
        var nodesOnBarrier = barrierMap.get(barrier);
        nodesOnBarrier.add(pair);
      }
    }
  }

  public OsmEntity getSomeOsmObject() {
    return areas.iterator().next().parent;
  }

  /**
   * Check if area group has a trivial geometry of one boundary ring and one respective area
   * In such a case it is known that the area and the boundary ring match
   *
   * @return true if area group consists of one polygion only
   */
  public boolean isSimpleAreaGroup() {
    return areas.size() == 1 && outermostRings.size() == 1;
  }

  private Ring toRing(Polygon polygon, HashMap<Coordinate, OsmNode> nodeMap) {
    List<OsmNode> shell = new ArrayList<>();
    for (Coordinate coord : polygon.getExteriorRing().getCoordinates()) {
      OsmNode node = nodeMap.get(coord);
      if (node == null) {
        throw new RingConstructionException();
      }
      shell.add(node);
    }
    Ring ring = new Ring(shell);
    // now the holes
    for (int i = 0; i < polygon.getNumInteriorRing(); ++i) {
      LineString interior = polygon.getInteriorRingN(i);
      List<OsmNode> hole = new ArrayList<>();
      for (Coordinate coord : interior.getCoordinates()) {
        OsmNode node = nodeMap.get(coord);
        if (node == null) {
          throw new RingConstructionException();
        }
        hole.add(node);
      }
      ring.addHole(new Ring(hole));
    }

    return ring;
  }
}
