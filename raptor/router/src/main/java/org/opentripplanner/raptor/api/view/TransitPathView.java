package org.opentripplanner.raptor.api.view;

import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public interface TransitPathView<T extends RaptorTripSchedule> {
  /**
   * Stop position in the pattern where the transit path was boarded.
   */
  int boardStopPosition();

  /**
   * Trip used for transit.
   */
  T trip();
}
