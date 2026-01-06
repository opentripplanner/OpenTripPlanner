package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmEntityForTest;
import org.opentripplanner.osm.model.TraverseDirection;

public class ConstantSpeedMapperTest {

  @Test
  public void constantSpeedCarRouting() {
    OsmTagMapper osmTagMapper = new ConstantSpeedMapper(20f);

    var slowWay = new OsmEntityForTest();
    slowWay.addTag("highway", "residential");
    assertEquals(20f, osmTagMapper.getCarSpeedForWay(slowWay, TraverseDirection.BACKWARD));

    var fastWay = new OsmEntityForTest();
    fastWay.addTag("highway", "motorway");
    fastWay.addTag("maxspeed", "120 kmph");
    assertEquals(20f, osmTagMapper.getCarSpeedForWay(fastWay, TraverseDirection.BACKWARD));
  }
}
