package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FeedScopedIdMapperTest {

  @Test
  public void testMapAgencyAndId() throws Exception {
    org.onebusaway.gtfs.model.AgencyAndId inputId = new org.onebusaway.gtfs.model.AgencyAndId(
      "A",
      "1"
    );

    FeedScopedId mappedId = mapAgencyAndId(inputId);

    assertEquals("A", mappedId.getFeedId());
    assertEquals("1", mappedId.getId());
  }

  @Test
  public void testMapAgencyAndIdWithNulls() throws Exception {
    assertThrows(IllegalArgumentException.class, () ->
      mapAgencyAndId(new org.onebusaway.gtfs.model.AgencyAndId())
    );
  }
}
