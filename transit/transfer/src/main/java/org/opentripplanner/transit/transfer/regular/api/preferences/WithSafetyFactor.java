package org.opentripplanner.transit.transfer.regular.api.preferences;

import org.opentripplanner.core.model.basic.NormalizedRatio;

/**
 * The preferences of a profile type where the traveler is on foot, and the safety of the streets is
 * considered. It is not meaningful for all types.
 */
public interface WithSafetyFactor {
  /**
   * How much the safety of the streets is considered, from 0.0 (ignored) to 1.0 (the safety factor
   * of the street is applied in whole as a multiplier on the cost).
   */
  NormalizedRatio safetyFactor();
}
