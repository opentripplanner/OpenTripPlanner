package org.opentripplanner.netex.mapping;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;

public class FeedScopedIdFactoryTest {

  @Test
  public void setFeedScope() {
    FeedScopedId feedScopedId1 = MappingSupport.ID_FACTORY.createId("NSR:StopPlace:1");
    assertEquals("F:NSR:StopPlace:1", feedScopedId1.toString());
  }
}
