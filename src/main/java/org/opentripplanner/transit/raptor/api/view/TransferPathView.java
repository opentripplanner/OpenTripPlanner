package org.opentripplanner.transit.raptor.api.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;

/**
 * Provide transfer path information to debugger and path mapping.
 */
public interface TransferPathView {

  RaptorTransfer transfer();

  /** The transfer duration in seconds */
  default int durationInSeconds() {
    return transfer().durationInSeconds();
  }
}
