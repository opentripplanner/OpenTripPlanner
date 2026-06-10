package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.TraverseDirection;

public class ConstantSpeedMapperTest {

  @Test
  public void constantSpeedCarRouting() {
    OsmTagMapper osmTagMapper = new ConstantSpeedMapper(20f);

    var slowWay = OsmWay.of().withTag("highway", "residential").build();
    assertEquals(
      20f,
      osmTagMapper.getCarSpeedForWay(slowWay, TraverseDirection.BACKWARD, DataImportIssueStore.NOOP)
    );

    var fastWay = OsmWay.of()
      .withTag("highway", "motorway")
      .withTag("maxspeed", "120 kmph")
      .build();
    assertEquals(
      20f,
      osmTagMapper.getCarSpeedForWay(fastWay, TraverseDirection.BACKWARD, DataImportIssueStore.NOOP)
    );
  }
}
