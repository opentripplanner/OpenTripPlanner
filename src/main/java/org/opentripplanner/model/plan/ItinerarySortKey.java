package org.opentripplanner.model.plan;

import java.time.ZonedDateTime;

public interface ItinerarySortKey {
  ZonedDateTime startTime();
  ZonedDateTime endTime();
  int getGeneralizedCost();
  int getNumberOfTransfers();
  boolean isOnStreetAllTheWay();
}
