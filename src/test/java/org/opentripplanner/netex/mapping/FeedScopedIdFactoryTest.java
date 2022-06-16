package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FeedScopedIdFactoryTest {

  @Test
  public void setFeedScope() {
    FeedScopedId feedScopedId1 = MappingSupport.ID_FACTORY.createId("NSR:StopPlace:1");
    assertEquals("F:NSR:StopPlace:1", feedScopedId1.toString());
  }
}
