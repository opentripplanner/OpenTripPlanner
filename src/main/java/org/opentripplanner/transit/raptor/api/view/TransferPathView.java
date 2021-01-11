package org.opentripplanner.transit.raptor.api.view;

/**
 * Provide transfer path information to debugger and path mapping.
 */
public interface TransferPathView {

  /** The transfer duration in seconds */
  int durationInSeconds();
}
