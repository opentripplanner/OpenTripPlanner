package org.opentripplanner.model.plan.itineraryreference;

import java.util.Objects;

/**
 * The preferences a client can set for a car rental access or egress, stored in an
 * {@link ItineraryReference}.
 *
 * @param reluctance The cost factor for time spent driving.
 * @param networks The rental networks to use.
 */
public record CarRentalPreferences(double reluctance, RentalNetworks networks) {
  public CarRentalPreferences {
    Objects.requireNonNull(networks);
  }
}
