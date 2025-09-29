package org.opentripplanner.graph_builder.module.osm.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CrosswalkNamerTest {

  private static final Logger LOG = LoggerFactory.getLogger(CrosswalkNamerTest.class);

  @Test
  void testGetIntersectingStreet() {
    OsmWay way = new OsmWay();
    way.getNodeRefs().add(new long[] { 10001, 10002, 10003, 10004 });
    OsmWay street = new OsmWay();
    street.setId(50001);
    street.getNodeRefs().add(new long[] { 20001, 20002, 20003, 10002, 20004, 20005 });
    OsmWay otherStreet = new OsmWay();
    otherStreet.setId(50002);
    otherStreet.getNodeRefs().add(new long[] { 30001, 30002, 30003, 30004, 30005 });

    var intersectingStreet = CrosswalkNamer.getIntersectingStreet(way, List.of(street, otherStreet));
    assertTrue(intersectingStreet.isPresent());
    assertEquals(50001, intersectingStreet.get().getId());

    var intersectingStreet2 = CrosswalkNamer.getIntersectingStreet(way, List.of(otherStreet));
    assertFalse(intersectingStreet2.isPresent());
  }
}
