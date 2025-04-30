package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class IdFactoryTest {

  private final IdFactory FACTORY = new IdFactory("B");

  @Test
  void createIdFromAgencyAndId() {
    org.onebusaway.gtfs.model.AgencyAndId inputId = new org.onebusaway.gtfs.model.AgencyAndId(
      "A",
      "1"
    );

    FeedScopedId mappedId = FACTORY.createId(inputId);

    assertEquals("B", mappedId.getFeedId());
    assertEquals("1", mappedId.getId());
  }

  @Test
  public void emptyAgencyAndId() {
    assertThrows(IllegalArgumentException.class, () ->
      FACTORY.createId(new org.onebusaway.gtfs.model.AgencyAndId())
    );
  }

  @Test
  public void nullAgencyAndId() {
    assertNull(FACTORY.createId((AgencyAndId) null));
  }
}
