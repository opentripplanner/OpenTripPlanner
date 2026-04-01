package org.opentripplanner.core.model.id;

public class FeedScopedIdFactory {

  public static FeedScopedId id(String id) {
    return new FeedScopedId("street", id);
  }

  public static FeedScopedId id(int id) {
    return id(String.valueOf(id));
  }
}
