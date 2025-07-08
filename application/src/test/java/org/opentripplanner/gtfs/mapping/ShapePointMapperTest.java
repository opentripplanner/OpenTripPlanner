package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.ShapePoint;

public class ShapePointMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

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
    SHAPE_POINT.setShapeId(AGENCY_AND_ID);
  }

  @Test
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<ShapePoint>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(SHAPE_POINT)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.model.ShapePoint result = subject.map(SHAPE_POINT);

    assertEquals(DIST_TRAVELED, result.getDistTraveled(), 0.0001d);
    assertEquals(LAT, result.getLat(), 0.0001d);
    assertEquals(LON, result.getLon(), 0.0001d);
    assertEquals(SEQUENCE, result.getSequence());
    assertEquals("A:1", result.getShapeId().toString());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    var orginal = new ShapePoint();
    orginal.setShapeId(AGENCY_AND_ID);
    org.opentripplanner.model.ShapePoint result = subject.map(orginal);

    assertFalse(result.isDistTraveledSet());
    assertEquals(0d, result.getLat(), 0.00001);
    assertEquals(0d, result.getLon(), 0.00001);
    assertEquals(0d, result.getSequence(), 0.00001);
    assertEquals("A:1", result.getShapeId().toString());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.model.ShapePoint result1 = subject.map(SHAPE_POINT);
    org.opentripplanner.model.ShapePoint result2 = subject.map(SHAPE_POINT);

    assertSame(result1, result2);
  }
}
