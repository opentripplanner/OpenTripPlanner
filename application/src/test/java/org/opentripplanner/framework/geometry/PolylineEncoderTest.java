package org.opentripplanner.framework.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class PolylineEncoderTest {

  @Test
  public void testCreateEncodingsOfCoordinateArray() {
    // test taken from example usage
    List<Coordinate> points = new ArrayList<>();
    points.add(new Coordinate(-73.85062, 40.903125, Double.NaN));
    points.add(new Coordinate(-73.85136, 40.902261, Double.NaN));
    points.add(new Coordinate(-73.85151, 40.902066, Double.NaN));
    EncodedPolyline eplb = PolylineEncoder.encodeCoordinates(points.toArray(new Coordinate[0]));
    assertEquals("o{sxFl}vaMjDpCf@\\", eplb.points());
    assertEquals(3, eplb.length());
  }

  @Test
  public void testPolygon() {
    var polygon = GeometryUtils.getGeometryFactory()
      .createPolygon(
        new Coordinate[] {
          new Coordinate(0, 0),
          new Coordinate(1, 1),
          new Coordinate(2, 2),
          new Coordinate(0, 0),
        }
      );
    var polyline = PolylineEncoder.encodeGeometry(polygon);

    assertEquals("??_ibE_ibE_ibE_ibE~reK~reK", polyline.points());
  }

  @Test
  public void testPoint() {
    var point = GeometryUtils.getGeometryFactory().createPoint(new Coordinate(100, 100));
    var polyline = PolylineEncoder.encodeGeometry(point);

    assertEquals("_gjaR_gjaR", polyline.points());
  }
}
