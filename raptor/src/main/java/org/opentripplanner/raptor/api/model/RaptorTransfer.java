package org.opentripplanner.raptor.api.model;

import org.opentripplanner.utils.time.DurationUtils;

/**
 * Encapsulate information about a transfer path.
 * Time dependent transfers are not supported.
 */
public interface RaptorTransfer {
  /**
   * Stop index where the path arrives at.
   * The journey origin, destination and transit path board stop must be part of the context;
   * hence not a member attribute of this type.
   */
  int stop();

  /**
   * The generalized cost of this transfer in centi-seconds.
   * <p>
   * This method is called many times, so care needs to be taken that the value is stored, not
   * calculated for each invocation.
   */
  int c1();

  /**
   * The time duration to walk or travel the path in seconds. This is not the entire duration from
   * the journey origin, but just stop to stop.
   */
  int durationInSeconds();

  /** Call this from toString */
  default String asString() {
    String duration = DurationUtils.durationToStr(durationInSeconds());
    return String.format("On-Street %s ~ %d", duration, stop());
  }
}
