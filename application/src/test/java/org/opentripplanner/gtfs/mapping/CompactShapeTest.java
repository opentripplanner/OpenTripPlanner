package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.ShapePoint;

class CompactShapeTest {

  @Test
  void simple() {
    var builder = new CompactShape();

    builder.addPoint(shapePoint(1, 1, 2));
    builder.addPoint(shapePoint(2, 2, 2));
    builder.addPoint(shapePoint(3, 3, 3));
    builder.addPoint(shapePoint(4, 4, 4));

    var points = ImmutableList.copyOf(builder);

    assertEquals("[1 (1.0, 2.0), 2 (2.0, 2.0), 3 (3.0, 3.0), 4 (4.0, 4.0)]", points.toString());
  }

  @Test
  void hole() {
    var builder = new CompactShape();

    var p1 = shapePoint(1, 1.0, 2.0);
    var p2 = shapePoint(2, 3.0, 4.0);
    var p3 = shapePoint(10, 3.0, 4.0);

    builder.addPoint(p1);
    builder.addPoint(p2);
    builder.addPoint(p3);

    var points = ImmutableList.copyOf(builder);

    assertEquals("[1 (1.0, 2.0), 2 (3.0, 4.0), 10 (3.0, 4.0)]", points.toString());
  }

  @Test
  void extendLatLon() {
    var builder = new CompactShape();

    builder.addPoint(shapePoint(1, 1, 1));
    builder.addPoint(shapePoint(2, 2, 2));
    builder.addPoint(shapePoint(10, 3, 3));
    builder.addPoint(shapePoint(51, 4, 4));

    var points = ImmutableList.copyOf(builder);

    assertEquals("[1 (1.0, 1.0), 2 (2.0, 2.0), 10 (3.0, 3.0), 51 (4.0, 4.0)]", points.toString());
  }

  @Test
  void shapeDist() {
    var builder = new CompactShape();

    builder.addPoint(shapePoint(1, 1, 1, 0d));
    builder.addPoint(shapePoint(2, 2, 2, 1d));
    builder.addPoint(shapePoint(10, 3, 3, 2d));
    builder.addPoint(shapePoint(51, 4, 4));

    var points = ImmutableList.copyOf(builder);

    assertEquals(
      "[1 (1.0, 1.0) dist=0.0, 2 (2.0, 2.0) dist=1.0, 10 (3.0, 3.0) dist=2.0, 51 (4.0, 4.0)]",
      points.toString()
    );
  }

  @Test
  void holeInDistTraveled() {
    var builder = new CompactShape();

    builder.addPoint(shapePoint(1, 1, 1, 1d));
    builder.addPoint(shapePoint(2, 2, 2, 2d));
    builder.addPoint(shapePoint(10, 3, 3, 4d));
    builder.addPoint(shapePoint(51, 4, 4));
    builder.addPoint(shapePoint(102, 5, 5, 10d));
    builder.addPoint(shapePoint(150, 6, 6));

    var points = ImmutableList.copyOf(builder);

    assertEquals(
      "[1 (1.0, 1.0) dist=1.0, 2 (2.0, 2.0) dist=2.0, 10 (3.0, 3.0) dist=4.0, 51 (4.0, 4.0), 102 (5.0, 5.0) dist=10.0, 150 (6.0, 6.0)]",
      points.toString()
    );
  }

  @Test
  void zero() {
    var builder = new CompactShape();

    builder.addPoint(shapePoint(0, 1, 1, 1d));
    builder.addPoint(shapePoint(9, 2, 2, 2d));
    builder.addPoint(shapePoint(10, 3, 3, 4d));
    builder.addPoint(shapePoint(50, 4, 4, 5d));
    builder.addPoint(shapePoint(51, 5, 5, 6d));

    var points = ImmutableList.copyOf(builder);

    assertEquals(
      "[0 (1.0, 1.0) dist=1.0, 9 (2.0, 2.0) dist=2.0, 10 (3.0, 3.0) dist=4.0, 50 (4.0, 4.0) dist=5.0, 51 (5.0, 5.0) dist=6.0]",
      points.toString()
    );
  }

  private static ShapePoint shapePoint(int sequence, double lat, double lon) {
    return shapePoint(sequence, lat, lon, null);
  }

  private static ShapePoint shapePoint(
    int sequence,
    double lat,
    double lon,
    @Nullable Double distTraveled
  ) {
    var p1 = new ShapePoint();
    p1.setSequence(sequence);
    p1.setLat(lat);
    p1.setLon(lon);
    if (distTraveled != null) {
      p1.setDistTraveled(distTraveled);
    }
    return p1;
  }
}
