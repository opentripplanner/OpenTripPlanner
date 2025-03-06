package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Heuristic data for a given stop.
 */
public record HeuristicAtStop(int minTravelDuration, int minNumTransfers, int minCost) {
  /**
   * Representation for a stop, which has not been reached by the heuristic search
   */
  public static final HeuristicAtStop UNREACHED = new HeuristicAtStop(
    RaptorConstants.UNREACHED_HIGH,
    RaptorConstants.UNREACHED_HIGH,
    RaptorConstants.UNREACHED_HIGH
  );

  @Override
  public String toString() {
    return this == UNREACHED
      ? "[]"
      : ("[" +
        (DurationUtils.durationToStr(minTravelDuration) + " ") +
        (minNumTransfers + "tx ") +
        OtpNumberFormat.formatCostCenti(minCost) +
        "]");
  }
}
