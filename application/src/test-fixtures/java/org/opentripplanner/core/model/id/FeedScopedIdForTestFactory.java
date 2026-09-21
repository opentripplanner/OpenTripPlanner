package org.opentripplanner.core.model.id;

public class FeedScopedIdForTestFactory {

  public static final String FEED_ID = "F";

  public static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }

  public static FeedScopedId id(int id) {
    return id(String.valueOf(id));
  }
}
