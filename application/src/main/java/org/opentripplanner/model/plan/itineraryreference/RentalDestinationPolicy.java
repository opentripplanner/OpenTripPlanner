package org.opentripplanner.model.plan.itineraryreference;

import java.util.Objects;
import org.opentripplanner.core.model.basic.Cost;

/**
 * Whether a client may keep a rented vehicle at the destination instead of returning it.
 *
 * @param allowKeeping Whether the vehicle may be kept at the destination.
 * @param keepingCost The cost of keeping it, to discourage doing so.
 */
public record RentalDestinationPolicy(boolean allowKeeping, Cost keepingCost) {
  public RentalDestinationPolicy {
    Objects.requireNonNull(keepingCost);
  }
}
