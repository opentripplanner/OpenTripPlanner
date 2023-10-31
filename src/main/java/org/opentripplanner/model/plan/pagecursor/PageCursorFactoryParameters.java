package org.opentripplanner.model.plan.pagecursor;

import java.time.Instant;

public interface PageCursorFactoryParameters {
  Instant earliestRemovedDeparture();

  Instant earliestKeptArrival();

  Instant latestRemovedDeparture();

  Instant latestRemovedArrival();
}
