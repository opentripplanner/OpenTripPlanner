package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;

/**
 * GTFS uses the term agency_id, which is used to scope the ids of all other elements in OTP. Since NeTEx does not
 * contain this id and also assumes ids are already unique, it is necessary to specify an id to use in the OTP model.
 * <p>
 * This factory is used to set the feed id once and then apply it to elements as they are created.
 */
public class FeedScopedIdFactory {
    private static String feedId = "NETEX_AGENCY_ID_NOT_SET";

    public static void setFeedId(String feedId) {
        FeedScopedIdFactory.feedId = feedId;
    }


    static FeedScopedId createFeedScopedId(String netexId) {
        return new FeedScopedId(feedId, netexId);
    }
}
