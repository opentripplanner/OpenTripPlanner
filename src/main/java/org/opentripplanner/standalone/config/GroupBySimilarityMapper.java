package org.opentripplanner.standalone.config;

import org.opentripplanner.routing.api.request.GroupBySimilarityParams;

import static org.opentripplanner.routing.api.request.GroupBySimilarityParams.DEFAULT;

public class GroupBySimilarityMapper {

  public static GroupBySimilarityParams map(NodeAdapter c) {
    return new GroupBySimilarityParams(
        c.asDouble("keepOne", DEFAULT.keepOne),
        c.asDouble("keepNumOfItineraries", DEFAULT.keepNumOfItineraries),
        c.asDouble("idealTransferTimeFactor", DEFAULT.idealTransferTimeFactor)
    );
  }
}
