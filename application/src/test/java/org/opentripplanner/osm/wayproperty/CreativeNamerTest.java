package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;

class CreativeNamerTest {

  @Test
  public void testCreativeNaming() {
    var way = OsmWay.of()
      .withTag("highway", "footway")
      .withTag("cycleway", "lane")
      .withTag("access", "no")
      .build();

    CreativeNamer namer = new CreativeNamer(
      "Highway with cycleway {cycleway} and access {access} and morx {morx}"
    );
    assertEquals(
      "Highway with cycleway lane and access no and morx ",
      namer.generateCreativeName(way).toString()
    );
  }
}
