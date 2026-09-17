package org.opentripplanner.core.model.id;

import org.opentripplanner.core.domain.model.id.FeedScopedId;

public class FeedScopedIdFactory {

  public static FeedScopedId id(String id) {
    return new FeedScopedId("street", id);
  }

  public static FeedScopedId id(int id) {
    return id(String.valueOf(id));
  }
}
