package org.opentripplanner.netex.mapping.support;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * GTFS uses the term agency_id, which is used to scope the ids of all other elements in OTP. Since
 * NeTEx does not contain this id and also assumes ids are already unique, it is necessary to
 * specify an id to use in the OTP model.
 * <p>
 * This factory is used to set the feed id once and then apply it to elements as they are created.
 * <p>
 * This class should only be used by the Netex import/mapping process.
 */
public class FeedScopedIdFactory {

  private final String feedId;

  public FeedScopedIdFactory(String feedId) {
    this.feedId = feedId;
  }

  public FeedScopedId createId(String netexId) {
    return new FeedScopedId(feedId, netexId);
  }
}
