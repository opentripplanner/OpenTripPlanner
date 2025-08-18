package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class ShapePointMapperTest {

  private static final FeedScopedId SHAPE_ID = new FeedScopedId("A", "5");

  private static final String CSV =
    """
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
    """;
  private final ShapePointMapper subject = new ShapePointMapper(new IdFactory("A"));

  @Test
  void testMap() {
    var result = map().getFirst();
    assertEquals(0, result.distTraveled(), 0.0001d);
    assertEquals(41, result.lat(), 0.0001d);
    assertEquals(-72, result.lon(), 0.0001d);
    assertEquals(1, result.sequence());
  }

  @Test
  void string() {
    var result = map().toString();
    assertEquals(
      "[1 (41.0, -72.0) dist=0.0, 2 (41.5, -72.5) dist=1.234, 3 (40.8075, -72.8075) dist=17.62, 4 (41.5, -73.5) dist=35.234, 5 (41.0, -74.0) dist=52.01]",
      result
    );
  }

  private List<ShapePoint> map() {
    final Map<FeedScopedId, CompactShape> map;
    try {
      map = subject.map(new TestCsvSource(CSV));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return ImmutableList.copyOf(map.get(SHAPE_ID));
  }
}
