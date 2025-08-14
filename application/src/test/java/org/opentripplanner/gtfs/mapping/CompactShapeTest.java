package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.ShapePoint;

class CompactShapeTest {

  @Test
  void simple() {
    var shape = new CompactShape();

    shape.addPoint(shapePoint(1, 1, 2));
    shape.addPoint(shapePoint(2, 2, 2));
    shape.addPoint(shapePoint(3, 3, 3));
    shape.addPoint(shapePoint(4, 4, 4));

    var points = ImmutableList.copyOf(shape);

    assertEquals("[1 (1.0, 2.0), 2 (2.0, 2.0), 3 (3.0, 3.0), 4 (4.0, 4.0)]", points.toString());
  }

  @Test
  void outOfSequence() {
    var shape = new CompactShape();

    shape.addPoint(shapePoint(4, 4, 4));
    shape.addPoint(shapePoint(1, 1, 2));
    shape.addPoint(shapePoint(3, 3, 3));
    shape.addPoint(shapePoint(2, 2, 2));

    var points = ImmutableList.copyOf(shape);

    assertEquals("[1 (1.0, 2.0), 2 (2.0, 2.0), 3 (3.0, 3.0), 4 (4.0, 4.0)]", points.toString());
  }

  @Test
  void hole() {
    var shape = new CompactShape();

    var p1 = shapePoint(1, 1.0, 2.0);
    var p2 = shapePoint(2, 3.0, 4.0);
    var p3 = shapePoint(10, 3.0, 4.0);

    shape.addPoint(p1);
    shape.addPoint(p2);
    shape.addPoint(p3);

    var points = ImmutableList.copyOf(shape);

    assertEquals("[1 (1.0, 2.0), 2 (3.0, 4.0), 10 (3.0, 4.0)]", points.toString());
  }

  @Test
  void extendLatLon() {
    var shape = new CompactShape();

    shape.addPoint(shapePoint(1, 1, 1));
    shape.addPoint(shapePoint(2, 2, 2));
    shape.addPoint(shapePoint(10, 3, 3));
    shape.addPoint(shapePoint(51, 4, 4));

    var points = ImmutableList.copyOf(shape);

    assertEquals("[1 (1.0, 1.0), 2 (2.0, 2.0), 10 (3.0, 3.0), 51 (4.0, 4.0)]", points.toString());
  }

  @Test
  void shapeDist() {
    var shape = new CompactShape();

    shape.addPoint(shapePoint(1, 1, 1, 0d));
    shape.addPoint(shapePoint(2, 2, 2, 1d));
    shape.addPoint(shapePoint(10, 3, 3, 2d));
    shape.addPoint(shapePoint(51, 4, 4));

    var points = ImmutableList.copyOf(shape);

    assertEquals(
      "[1 (1.0, 1.0) dist=0.0, 2 (2.0, 2.0) dist=1.0, 10 (3.0, 3.0) dist=2.0, 51 (4.0, 4.0)]",
      points.toString()
    );
  }

  @Test
  void holeInDistTraveled() {
    var shape = new CompactShape();

    shape.addPoint(shapePoint(1, 1, 1, 1d));
    shape.addPoint(shapePoint(2, 2, 2, 2d));
    shape.addPoint(shapePoint(51, 4, 4));
    shape.addPoint(shapePoint(102, 5, 5, 10d));
    shape.addPoint(shapePoint(150, 6, 6));
    shape.addPoint(shapePoint(10, 3, 3, 4d));

    var points = ImmutableList.copyOf(shape);

    assertEquals(
      "[1 (1.0, 1.0) dist=1.0, 2 (2.0, 2.0) dist=2.0, 10 (3.0, 3.0) dist=4.0, 51 (4.0, 4.0), 102 (5.0, 5.0) dist=10.0, 150 (6.0, 6.0)]",
      points.toString()
    );
  }

  @Test
  void zero() {
    var shape = new CompactShape();

    shape.addPoint(shapePoint(0, 1, 1, 1d));
    shape.addPoint(shapePoint(9, 2, 2, 2d));
    shape.addPoint(shapePoint(10, 3, 3, 4d));
    shape.addPoint(shapePoint(50, 4, 4, 5d));
    shape.addPoint(shapePoint(51, 5, 5, 6d));

    var points = ImmutableList.copyOf(shape);

    assertEquals(
      "[0 (1.0, 1.0) dist=1.0, 9 (2.0, 2.0) dist=2.0, 10 (3.0, 3.0) dist=4.0, 50 (4.0, 4.0) dist=5.0, 51 (5.0, 5.0) dist=6.0]",
      points.toString()
    );
  }

  @Test
  void largeSeq() {
    var shape = new CompactShape();

    shape.addPoint(shapePoint(0, 1, 1, 1d));
    shape.addPoint(shapePoint(10_000, 2, 2, 2d));
    shape.addPoint(shapePoint(30_000, 3, 3, 4d));
    shape.addPoint(shapePoint(40_000_000, 4, 4, 5d));
    shape.addPoint(shapePoint(Integer.MAX_VALUE, 5, 5, 6d));

    var points = ImmutableList.copyOf(shape);

    assertEquals(
      "[0 (1.0, 1.0) dist=1.0, 10,000 (2.0, 2.0) dist=2.0, 30,000 (3.0, 3.0) dist=4.0, 40,000,000 (4.0, 4.0) dist=5.0, 2,147,483,647 (5.0, 5.0) dist=6.0]",
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
