package org.opentripplanner.raptor.api.model;

import org.opentripplanner.framework.time.DurationUtils;

/**
 * Encapsulate information about a transfer path.
 */
public interface RaptorTransfer {
  /**
   * Stop index where the path arrives at.
   * The journey origin, destination and transit path board stop must be part of the context;
   * hence not a member attribute of this type.
   */
  int stop();

  /**
   * The generalized cost of this transfer in centi-seconds. The value is used to compare with
   * riding transit, and will be one component of a full itinerary.
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

  /* TIME-DEPENDENT ACCESS/TRANSFER/EGRESS */
  // The methods below should be only overridden when a RaptorTransfer is only available at
  // specific times, such as flexible transit, TNC or shared vehicle schemes with limited opening
  // hours, not for regular access/transfer/egress.

  /** Call this from toString */
  default String asString() {
    String duration = DurationUtils.durationToStr(durationInSeconds());
    return String.format("On-Street %s ~ %d", duration, stop());
  }
}
