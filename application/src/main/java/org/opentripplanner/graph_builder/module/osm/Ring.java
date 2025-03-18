package org.opentripplanner.graph_builder.module.osm;

import gnu.trove.list.TLongList;
import gnu.trove.map.TLongObjectMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.framework.geometry.CoordinateArrayListSequence;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.osm.model.OsmNode;

class Ring {

  private final LinearRing shell;
  private final List<Ring> holes = new ArrayList<>();
  public List<OsmNode> nodes;
  // equivalent to the ring representation, but used for JTS operations
  public Polygon jtsPolygon;

  public Ring(List<OsmNode> osmNodes) {
    ArrayList<Coordinate> vertices = new ArrayList<>();
    nodes = osmNodes;
    for (OsmNode node : osmNodes) {
      Coordinate point = new Coordinate(node.lon, node.lat);
      vertices.add(point);
    }
    // Make sure rings are always clockwise, in order to be able to calculate if it is concave/convex
    if (Orientation.isCCW(new CoordinateArrayListSequence(vertices))) {
      nodes = new ArrayList<>(nodes);
      Collections.reverse(nodes);
      Collections.reverse(vertices);
    }
    GeometryFactory factory = GeometryUtils.getGeometryFactory();
    try {
      shell = factory.createLinearRing(vertices.toArray(new Coordinate[0]));
    } catch (IllegalArgumentException e) {
      throw new RingConstructionException();
    }
    jtsPolygon = calculateJtsPolygon();
  }

  public Ring(TLongList osmNodes, TLongObjectMap<OsmNode> _nodes) {
    // The collection needs to be mutable, so collect into an ArrayList
    this(
      LongStream.of(osmNodes.toArray())
        .mapToObj(_nodes::get)
        .collect(Collectors.toCollection(ArrayList::new))
    );
  }

  public List<Ring> getHoles() {
    return holes;
  }

  public void addHole(Ring hole) {
    holes.add(hole);
    jtsPolygon = calculateJtsPolygon();
  }

  /**
   * Checks whether the ith node in the ring is convex (has an angle of over 180 degrees).
   */
  boolean isNodeConvex(int i) {
    int n = nodes.size() - 1;
    OsmNode cur = nodes.get(i);
    OsmNode prev = nodes.get((i + n - 1) % n);
    OsmNode next = nodes.get((i + 1) % n);
    return (
      (cur.lon - prev.lon) * (next.lat - cur.lat) - (cur.lat - prev.lat) * (next.lon - cur.lon) >
      0.00000000001
    );
  }

  private Polygon calculateJtsPolygon() {
    GeometryFactory factory = GeometryUtils.getGeometryFactory();

    // we need to merge connected holes here, because JTS does not believe in
    // holes that touch at multiple points (and, weirdly, does not have a method
    // to detect this other than this crazy DE-9IM stuff

    List<Polygon> polygonHoles = new ArrayList<>();
    for (Ring ring : holes) {
      Polygon polygon = factory.createPolygon(ring.shell, new LinearRing[0]);
      for (Iterator<Polygon> it = polygonHoles.iterator(); it.hasNext();) {
        Polygon otherHole = it.next();
        if (otherHole.relate(polygon, "F***1****")) {
          polygon = (Polygon) polygon.union(otherHole);
          it.remove();
        }
      }
      polygonHoles.add(polygon);
    }

    ArrayList<LinearRing> lrholelist = new ArrayList<>(polygonHoles.size());

    for (Polygon hole : polygonHoles) {
      Geometry boundary = hole.getBoundary();
      if (boundary instanceof LinearRing) {
        lrholelist.add((LinearRing) boundary);
      } else {
        // this is a case of a hole inside a hole. OSM technically
        // allows this, but it would be a giant hassle to get right. So:
        LineString line = hole.getExteriorRing();
        LinearRing ring = factory.createLinearRing(line.getCoordinates());
        lrholelist.add(ring);
      }
    }
    LinearRing[] lrholes = lrholelist.toArray(new LinearRing[0]);
    return factory.createPolygon(shell, lrholes);
  }

  public static class RingConstructionException extends RuntimeException {}
}
