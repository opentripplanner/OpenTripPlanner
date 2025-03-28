package org.opentripplanner.routing.algorithm.mapping._support.mapping;

import org.opentripplanner.transit.model.framework.FeedScopedId;

@Deprecated
class FeedScopedIdMapper {

  private static final String SEPARATOR = ":";

  public static String mapToApi(FeedScopedId arg) {
    if (arg == null) {
      return null;
    }
    return arg.getFeedId() + SEPARATOR + arg.getId();
  }
}
