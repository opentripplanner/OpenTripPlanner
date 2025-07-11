package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tngtech.archunit.thirdparty.com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.ShapePoint;

class CompactShapeBuilderTest {

  @Test
  void hole() {
    var builder = new CompactShapeBuilder();

    var p1 = shapePoint(1, 1.0, 2.0);
    var p2 = shapePoint(2, 3.0, 4.0);
    var p3 = shapePoint(10, 3.0, 4.0);

    builder.addPoint(p1);
    builder.addPoint(p2);
    builder.addPoint(p3);

    var points = ImmutableList.copyOf(builder.shapePoints());

    assertEquals("[1 (1.0, 2.0), 2 (3.0, 4.0), 10 (3.0, 4.0)]", points.toString());
  }

  @Test
  void extendLatLon() {
    var builder = new CompactShapeBuilder();

    builder.addPoint(shapePoint(1, 1, 1));
    builder.addPoint(shapePoint(2, 2, 2));
    builder.addPoint(shapePoint(10, 3, 3));
    builder.addPoint(shapePoint(51, 4, 4));

    var points = ImmutableList.copyOf(builder.shapePoints());

    assertEquals("[1 (1.0, 1.0), 2 (2.0, 2.0), 10 (3.0, 3.0), 51 (4.0, 4.0)]", points.toString());
  }

  private static ShapePoint shapePoint(int sequence, double lat, double lon) {
    var p1 = new ShapePoint();
    p1.setSequence(sequence);
    p1.setLat(lat);
    p1.setLon(lon);
    return p1;
  }
}
