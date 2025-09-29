package org.opentripplanner.graph_builder.module.osm.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CrosswalkNamerTest {

  private static final Logger LOG = LoggerFactory.getLogger(CrosswalkNamerTest.class);

  private static final OsmWay CROSSWALK = new OsmWay();
  private static final OsmWay STREET = new OsmWay();
  private static final OsmWay OTHER_STREET = new OsmWay();


  @BeforeAll
  static void setUp() {
    CROSSWALK.addTag("highway", "footway");
    CROSSWALK.getNodeRefs().add(new long[] { 10001, 10002, 10003, 10004 });

    STREET.setId(50001);
    STREET.addTag("highway", "primary");
    STREET.addTag("name", "3rd Street");
    STREET.getNodeRefs().add(new long[] { 20001, 20002, 20003, 10002, 20004, 20005 });

    OTHER_STREET.setId(50002);
    OTHER_STREET.getNodeRefs().add(new long[] { 30001, 30002, 30003, 30004, 30005 });
  }

  @Test
  void testGetIntersectingStreet() {
    var intersectingStreet = CrosswalkNamer.getIntersectingStreet(CROSSWALK, List.of(STREET, OTHER_STREET));
    assertTrue(intersectingStreet.isPresent());
    assertEquals(50001, intersectingStreet.get().getId());

    var intersectingStreet2 = CrosswalkNamer.getIntersectingStreet(CROSSWALK, List.of(OTHER_STREET));
    assertFalse(intersectingStreet2.isPresent());
  }
}
