package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class ShapePointMapperTest {

  private static final AgencyAndId OBA_ID = new AgencyAndId("A", "1");
  private static final FeedScopedId SHAPE_ID = new FeedScopedId("A", "1");

  private static final Integer ID = 45;

  private static final double DIST_TRAVELED = 2.0d;

  private static final double LAT = 60.0d;

  private static final double LON = 45.0d;

  private static final int SEQUENCE = 3;

  private static final ShapePoint SHAPE_POINT = new ShapePoint();
  private final ShapePointMapper subject = new ShapePointMapper(new IdFactory("A"));

  static {
    SHAPE_POINT.setId(ID);
    SHAPE_POINT.setDistTraveled(DIST_TRAVELED);
    SHAPE_POINT.setLat(LAT);
    SHAPE_POINT.setLon(LON);
    SHAPE_POINT.setSequence(SEQUENCE);
    SHAPE_POINT.setShapeId(OBA_ID);
  }

  @Test
  public void testMapCollection() {
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(SHAPE_POINT)).size());
  }

  @Test
  public void testMap() {
    var result = map(List.of(SHAPE_POINT));
    assertEquals(DIST_TRAVELED, result.distTraveled(), 0.0001d);
    assertEquals(LAT, result.lat(), 0.0001d);
    assertEquals(LON, result.lon(), 0.0001d);
    assertEquals(SEQUENCE, result.sequence());
  }

  @Test
  void string() {
    var result = map(List.of(SHAPE_POINT)).toString();
    assertEquals("3 (60.0, 45.0) dist=2.0", result);
  }

  private org.opentripplanner.model.ShapePoint map(List<ShapePoint> shapePoint) {
    final Map<FeedScopedId, CompactShape> map = subject.map(shapePoint);
    return ImmutableList.copyOf(map.get(SHAPE_ID)).getFirst();
  }
}
