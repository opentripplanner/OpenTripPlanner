package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;

// TODO OTP2 - JavaDoc needed
// TODO OTP2 - Add Unit tests
public class FeedScopedIdFactory {
    private static String feedId = "NETEX_AGENCY_ID_NOT_SET";

    public static void setFeedId(String feedId) {
        FeedScopedIdFactory.feedId = feedId;
    }


    static FeedScopedId createFeedScopedId(String netexId) {
        return new FeedScopedId(feedId, netexId);
    }
}
