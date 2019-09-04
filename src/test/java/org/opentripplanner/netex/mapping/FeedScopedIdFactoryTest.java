package org.opentripplanner.netex.mapping;

import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;

import static org.junit.Assert.*;

public class FeedScopedIdFactoryTest {
    @Test
    public void setFeedScope() {
        FeedScopedId feedScopedId1 = FeedScopedIdFactory.createFeedScopedId("NSR:StopPlace:1");
        FeedScopedIdFactory.setFeedId("RB");
        FeedScopedId feedScopedId2 = FeedScopedIdFactory.createFeedScopedId("NSR:StopPlace:2");

        assertEquals("NETEX_AGENCY_ID_NOT_SET_NSR:StopPlace:1", feedScopedId1.toString());
        assertEquals("RB_NSR:StopPlace:2", feedScopedId2.toString());
    }
}