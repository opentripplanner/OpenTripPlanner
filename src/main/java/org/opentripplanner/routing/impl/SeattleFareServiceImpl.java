package org.opentripplanner.routing.impl;

import com.google.common.collect.Iterables;
import org.opentripplanner.model.FeedScopedId;

import java.util.List;

public class SeattleFareServiceImpl extends DefaultFareServiceImpl {
    private static final long serialVersionUID = 2L;

    private static final String KCM_FEED_ID = "1";
    private static final String KCM_AGENCY_ID = "1";

    @Override
    protected float addFares(List<Ride> ride0, List<Ride> ride1, float cost0, float cost1) {
        String feedId = ride0.get(0).firstStop.getId().getFeedId();
        FeedScopedId agencyId = ride0.get(0).agency;
        if (KCM_FEED_ID.equals(feedId) && KCM_AGENCY_ID.equals(agencyId.getId())) {
            for (Ride r : Iterables.concat(ride0, ride1)) {
                if (!isCorrectAgency(r, feedId, agencyId)) {
                    return cost0 + cost1;
                }
            }
            return Math.max(cost0, cost1);
        }
        return cost0 + cost1;
    }

    private static boolean isCorrectAgency(Ride r, String feedId, FeedScopedId agencyId) {
        String rideFeedId = r.firstStop.getId().getFeedId();
        FeedScopedId rideAgencyId = r.agency;
        return feedId.equals(rideFeedId) && agencyId.equals(rideAgencyId);
    }

}
