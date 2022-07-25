package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.model.site.StopLocation;

public class GroupByUtils {

  /**
   * Get the parent station id if such exists. Otherwise, return the stop id.
   */
  protected static FeedScopedId getStopOrStationId(StopLocation stopPlace) {
    if (stopPlace instanceof StationElement stationElement && stationElement.isPartOfStation()) {
      return stationElement.getParentStation().getId();
    }
    return stopPlace.getId();
  }
}
