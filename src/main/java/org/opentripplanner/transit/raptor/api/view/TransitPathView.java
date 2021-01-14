package org.opentripplanner.transit.raptor.api.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

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
