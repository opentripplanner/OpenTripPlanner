package org.opentripplanner.transit.model._data;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FeedScopedIdForTestFactory {

  public static final String FEED_ID = "F";

  public static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }
}
