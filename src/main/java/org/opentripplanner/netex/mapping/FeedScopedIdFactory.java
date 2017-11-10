package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;

class FeedScopedIdFactory {
    static FeedScopedId createAgencyAndId(String netexId) {
        return new FeedScopedId("RB", netexId);
    }
}
