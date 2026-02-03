package org.opentripplanner.api.model.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.framework.geometry.GeometryUtils;

class EncodedPolylineTest {

  private static final Geometry GEOMETRY = GeometryUtils.getGeometryFactory().createLineString(
    new Coordinate[] { new Coordinate(60.0, 9.0), new Coordinate(63.1, 10.3) }
  );

  private EncodedPolyline subject = EncodedPolyline.of(GEOMETRY);

  @Test
  void points() {
    assertEquals("_y|u@_wemJ_||F_n|Q", subject.points());
  }

  @Test
  void length() {
    assertEquals(2, subject.length());
  }

  @Test
  void testEqualsAndHashCode() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(EncodedPolyline.of(GEOMETRY))
      .differentFrom(EncodedPolyline.of(GeometryUtils.makeLineString(67, 10, 68, 11)));
  }

  @Test
  void testToString() {
    assertEquals("EncodedPolyline{length: 2, points: _y|u@_wemJ_||F_n|Q}", subject.toString());
  }
}
