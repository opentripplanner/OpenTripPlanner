package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class IdFactoryTest {

  private final IdFactory FACTORY = new IdFactory("B");

  @Test
  void testToId() {
    org.onebusaway.gtfs.model.AgencyAndId inputId = new org.onebusaway.gtfs.model.AgencyAndId(
      "A",
      "1"
    );

    FeedScopedId mappedId = FACTORY.toId(inputId);

    assertEquals("B", mappedId.getFeedId());
    assertEquals("1", mappedId.getId());
  }

  @Test
  public void testToIdWithNulls() {
    assertThrows(IllegalArgumentException.class, () ->
      FACTORY.toId(new org.onebusaway.gtfs.model.AgencyAndId())
    );
  }
}
