package org.opentripplanner.raptor.api.view;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

public interface TransitPathView<T extends RaptorTripSchedule> {
  /**
   * Stop index where the transit path was boarded.
   */
  int boardStop();

  /**
   * Trip used for transit.
   */
  T trip();
}
