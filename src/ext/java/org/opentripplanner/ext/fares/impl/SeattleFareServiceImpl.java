package org.opentripplanner.ext.fares.impl;

import com.google.common.collect.Iterables;
import java.util.List;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class SeattleFareServiceImpl extends DefaultFareServiceImpl {

  private static final long serialVersionUID = 2L;

  private static final String KCM_FEED_ID = "1";
  private static final String KCM_AGENCY_ID = "1";

  @Override
  protected float addFares(List<Leg> ride0, List<Leg> ride1, float cost0, float cost1) {
    String feedId = ride0.get(0).getFrom().stop.getId().getFeedId();
    FeedScopedId agencyId = ride0.get(0).getAgency().getId();
    if (KCM_FEED_ID.equals(feedId) && KCM_AGENCY_ID.equals(agencyId.getId())) {
      for (var r : Iterables.concat(ride0, ride1)) {
        if (!isCorrectAgency(r, feedId, agencyId)) {
          return cost0 + cost1;
        }
      }
      return Math.max(cost0, cost1);
    }
    return cost0 + cost1;
  }

  private static boolean isCorrectAgency(Leg r, String feedId, FeedScopedId agencyId) {
    String rideFeedId = r.getFrom().stop.getId().getFeedId();
    FeedScopedId rideAgencyId = r.getAgency().getId();
    return feedId.equals(rideFeedId) && agencyId.equals(rideAgencyId);
  }
}
