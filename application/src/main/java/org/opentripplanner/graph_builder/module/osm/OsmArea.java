package org.opentripplanner.graph_builder.module.osm;

import com.google.common.collect.ArrayListMultimap;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TLongObjectMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmNode;
import org.opentripplanner.osm.model.OsmWay;

/**
 * Stores information about an OSM area needed for visibility graph construction. Algorithm based on
 * http://wiki.openstreetmap.org/wiki/Relation:multipolygon/Algorithm but generally done in a
 * quick/dirty way.
 */
class OsmArea {

  final List<Ring> outermostRings;
  // This is the way or relation that has the relevant tags for the area
  final OsmEntity parent;
  public MultiPolygon jtsMultiPolygon;

  OsmArea(
    OsmEntity parent,
    List<OsmWay> outerRingWays,
    List<OsmWay> innerRingWays,
    TLongObjectMap<OsmNode> nodes
  ) {
    this.parent = parent;
    // ring assignment
    List<TLongList> innerRingNodes = constructRings(innerRingWays);
    List<TLongList> outerRingNodes = constructRings(outerRingWays);
    if (innerRingNodes == null || outerRingNodes == null) {
      throw new AreaConstructionException();
    }
    ArrayList<TLongList> allRings = new ArrayList<>(innerRingNodes);
    allRings.addAll(outerRingNodes);

    List<Ring> innerRings = new ArrayList<>();
    List<Ring> outerRings = new ArrayList<>();
    for (TLongList ring : innerRingNodes) {
      innerRings.add(new Ring(ring, nodes));
    }
    for (TLongList ring : outerRingNodes) {
      outerRings.add(new Ring(ring, nodes));
    }

    List<Ring> outermostRings = new ArrayList<>();

    try {
      // now, ring grouping
      // first, find outermost rings
      OUTER:for (Ring outer : outerRings) {
        for (Ring possibleContainer : outerRings) {
          if (outer != possibleContainer && outer.jtsPolygon.within(possibleContainer.jtsPolygon)) {
            continue OUTER;
          }
        }
        outermostRings.add(outer);

        // find holes in this ring
        for (Ring possibleHole : innerRings) {
          if (possibleHole.jtsPolygon.within(outer.jtsPolygon)) {
            outer.addHole(possibleHole);
          }
        }
      }
    } catch (TopologyException ex) {
      throw new AreaConstructionException();
    }

    // Make outermostRings immutable
    this.outermostRings = List.copyOf(outermostRings);
    // run this at end of ctor so that exception
    // can be caught in the right place
    jtsMultiPolygon = calculateJTSMultiPolygon();
  }

  public List<TLongList> constructRings(List<OsmWay> ways) {
    if (ways.size() == 0) {
      // no rings is no rings
      return Collections.emptyList();
    }

    List<TLongList> closedRings = new ArrayList<>();

    ArrayListMultimap<Long, OsmWay> waysByEndpoint = ArrayListMultimap.create();
    for (OsmWay way : ways) {
      TLongList refs = way.getNodeRefs();

      long start = refs.get(0);
      long end = refs.get(refs.size() - 1);
      if (start == end) {
        TLongList ring = new TLongArrayList(refs);
        closedRings.add(ring);
      } else {
        waysByEndpoint.put(start, way);
        waysByEndpoint.put(end, way);
      }
    }

    // Precheck for impossible situations, and remove those.
    TLongList endpointsToRemove = new TLongArrayList();
    for (Long endpoint : waysByEndpoint.keySet()) {
      Collection<OsmWay> list = waysByEndpoint.get(endpoint);
      if (list.size() % 2 == 1) {
        endpointsToRemove.add(endpoint);
      }
    }
    endpointsToRemove.forEach(endpoint -> {
      waysByEndpoint.removeAll(endpoint);
      return true;
    });

    TLongList partialRing = new TLongArrayList();
    if (waysByEndpoint.size() == 0) {
      return closedRings;
    }

    long firstEndpoint = 0, otherEndpoint = 0;
    OsmWay firstWay = null;
    for (Long endpoint : waysByEndpoint.keySet()) {
      List<OsmWay> list = waysByEndpoint.get(endpoint);
      firstWay = list.get(0);
      TLongList nodeRefs = firstWay.getNodeRefs();
      partialRing.addAll(nodeRefs);
      firstEndpoint = nodeRefs.get(0);
      otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
      break;
    }
    waysByEndpoint.get(firstEndpoint).remove(firstWay);
    waysByEndpoint.get(otherEndpoint).remove(firstWay);
    if (constructRingsRecursive(waysByEndpoint, partialRing, closedRings, firstEndpoint)) {
      return closedRings;
    } else {
      return null;
    }
  }

  /**
   * Try to extract a point which is in  the middle of the area and
   * also insaide the area geometry.
   *
   * @return Point geometry inside the area
   */
  public Point findInteriorPoint() {
    var centroid = jtsMultiPolygon.getCentroid();
    if (jtsMultiPolygon.intersects(centroid)) {
      return centroid;
    }
    return jtsMultiPolygon.getInteriorPoint();
  }

  private MultiPolygon calculateJTSMultiPolygon() {
    List<Polygon> polygons = new ArrayList<>();
    for (Ring ring : outermostRings) {
      polygons.add(ring.jtsPolygon);
    }
    MultiPolygon jtsMultiPolygon = GeometryUtils
      .getGeometryFactory()
      .createMultiPolygon(polygons.toArray(new Polygon[0]));
    if (!jtsMultiPolygon.isValid()) {
      throw new AreaConstructionException();
    }

    return jtsMultiPolygon;
  }

  private boolean constructRingsRecursive(
    ArrayListMultimap<Long, OsmWay> waysByEndpoint,
    TLongList ring,
    List<TLongList> closedRings,
    long endpoint
  ) {
    List<OsmWay> ways = new ArrayList<>(waysByEndpoint.get(endpoint));

    for (OsmWay way : ways) {
      // remove this way from the map
      TLongList nodeRefs = way.getNodeRefs();
      long firstEndpoint = nodeRefs.get(0);
      long otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);

      waysByEndpoint.remove(firstEndpoint, way);
      waysByEndpoint.remove(otherEndpoint, way);

      TLongList newRing = new TLongArrayList(ring.size() + nodeRefs.size());
      long newFirstEndpoint;
      if (firstEndpoint == endpoint) {
        for (int j = nodeRefs.size() - 1; j >= 1; --j) {
          newRing.add(nodeRefs.get(j));
        }
        newRing.addAll(ring);
        newFirstEndpoint = otherEndpoint;
      } else {
        newRing.addAll(nodeRefs.subList(0, nodeRefs.size() - 1));
        newRing.addAll(ring);
        newFirstEndpoint = firstEndpoint;
      }
      if (newRing.get(newRing.size() - 1) == (newRing.get(0))) {
        // ring closure
        closedRings.add(newRing);
        // if we're out of endpoints, then we have succeeded
        if (waysByEndpoint.size() == 0) {
          return true; // success
        }

        // otherwise, we need to start a new partial ring
        newRing = new TLongArrayList();
        OsmWay firstWay = null;
        for (Long entry : waysByEndpoint.keySet()) {
          List<OsmWay> list = waysByEndpoint.get(entry);
          firstWay = list.get(0);
          nodeRefs = firstWay.getNodeRefs();
          newRing.addAll(nodeRefs);
          firstEndpoint = nodeRefs.get(0);
          otherEndpoint = nodeRefs.get(nodeRefs.size() - 1);
          break;
        }

        waysByEndpoint.remove(firstEndpoint, firstWay);
        waysByEndpoint.remove(otherEndpoint, firstWay);

        if (constructRingsRecursive(waysByEndpoint, newRing, closedRings, firstEndpoint)) {
          return true;
        }

        waysByEndpoint.remove(firstEndpoint, firstWay);
        waysByEndpoint.remove(otherEndpoint, firstWay);
      } else {
        // continue with this ring
        if (waysByEndpoint.get(newFirstEndpoint) != null) {
          if (constructRingsRecursive(waysByEndpoint, newRing, closedRings, newFirstEndpoint)) {
            return true;
          }
        }
      }
      if (firstEndpoint == endpoint) {
        waysByEndpoint.put(otherEndpoint, way);
      } else {
        waysByEndpoint.put(firstEndpoint, way);
      }
    }
    return false;
  }

  public static class AreaConstructionException extends RuntimeException {}
}
