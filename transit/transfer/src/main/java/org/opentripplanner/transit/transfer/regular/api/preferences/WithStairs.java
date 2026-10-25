package org.opentripplanner.transit.transfer.regular.api.preferences;

import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;

/**
 * The preferences of a profile type where the traveler is on foot, or pushes a vehicle, on stairs.
 * It is not meaningful for all types, a car has no stairs.
 */
public interface WithStairs {
  /** Whether stairs are allowed, discouraged or forbidden. */
  StreetSegmentTypeUsage stairs();
}
