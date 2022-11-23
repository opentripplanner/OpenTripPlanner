package org.opentripplanner.raptor.api.view;

import org.opentripplanner.raptor.spi.RaptorTransfer;

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
