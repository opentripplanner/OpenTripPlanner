package org.opentripplanner.street.model._data;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;

public class StreetModelForTest {

  public static StreetVertex V1 = intersectionVertex("V1", 0, 0);
  public static StreetVertex V2 = intersectionVertex("V2", 1, 1);
  public static StreetVertex V3 = intersectionVertex("V3", 2, 2);
  public static StreetVertex V4 = intersectionVertex("V4", 3, 3);

  public static IntersectionVertex intersectionVertex(double lat, double lon) {
    var label = "%s_%s".formatted(lat, lon);
    return new IntersectionVertex(label, lat, lon, label);
  }

  public static IntersectionVertex intersectionVertex(String label, double lat, double lon) {
    return new IntersectionVertex(label, lat, lon, label);
  }

  public static StreetEdge streetEdge(StreetVertex vA, StreetVertex vB) {
    var meters = SphericalDistanceLibrary.distance(vA.getCoordinate(), vB.getCoordinate());
    return streetEdge(vA, vB, meters, StreetTraversalPermission.ALL);
  }

  public static StreetEdge streetEdge(
    StreetVertex vA,
    StreetVertex vB,
    double length,
    StreetTraversalPermission perm
  ) {
    String labelA = vA.getLabel();
    String labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    return new StreetEdge(vA, vB, geom, name, length, perm, false);
  }
}
