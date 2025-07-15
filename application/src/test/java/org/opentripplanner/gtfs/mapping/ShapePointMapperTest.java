package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.onebusaway.csv_entities.CsvInputSource;
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
  public void testMap() {
    var result = map();
    assertEquals(DIST_TRAVELED, result.distTraveled(), 0.0001d);
    assertEquals(LAT, result.lat(), 0.0001d);
    assertEquals(LON, result.lon(), 0.0001d);
    assertEquals(SEQUENCE, result.sequence());
  }

  @Test
  void string() {
    var result = map().toString();
    assertEquals("3 (60.0, 45.0) dist=2.0", result);
  }

  private org.opentripplanner.model.ShapePoint map() {
    final Map<FeedScopedId, CompactShape> map;
    try {
      map = subject.map(new ShapePointSource());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return ImmutableList.copyOf(map.get(SHAPE_ID)).getFirst();
  }

  static class ShapePointSource implements CsvInputSource {

    @Override
    public boolean hasResource(String name) {
      return true;
    }

    @Override
    public InputStream getResource(String name) {
      var s ="""
        shape_id,shape_pt_lat,shape_pt_lon,shape_pt_sequence,shape_dist_traveled
        4,41.0,-75.0,1,
        4,42.0,-75.0,2,
        4,42.5,-75.3,3,
        4,43.0,-75.0,4,
        5,41.0,-72.0,1,0
        5,41.5,-72.5,2,1.234
        5,40.8075,-72.8075,3,17.62
        5,41.5,-73.5,4,35.234
        5,41.0,-74.0,5,52.01
        """.stripIndent();
      return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {

    }
  }
}
