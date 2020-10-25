package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;

import static org.junit.Assert.assertEquals;

public class FeedScopedIdFactoryTest {
    @Test
    public void setFeedScope() {
        FeedScopedId feedScopedId1 = MappingSupport.ID_FACTORY.createId("NSR:StopPlace:1");
        assertEquals("F:NSR:StopPlace:1", feedScopedId1.toString());
    }
}