package org.opentripplanner.routing.api.request.preference.filter;

import java.util.Collections;
import java.util.Set;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

/**
 * A set of conditions that can be used to check if a parking facility should be included/excluded
 * or preferred/unpreferred.
 */
public sealed interface VehicleParkingSelect {
  /**
   * Checks if the parking facilities matches the conditions of the select.
   */
  boolean matches(VehicleParking p);

  /**
   * Whether this select defines any condition.
   */
  boolean isEmpty();

  record TagsSelect(Set<String> tags) implements VehicleParkingSelect {
    @Override
    public boolean matches(VehicleParking p) {
      return !Collections.disjoint(tags, p.getTags());
    }

    @Override
    public boolean isEmpty() {
      return tags.isEmpty();
    }

    @Override
    public String toString() {
      return "tags=" + tags.stream().sorted().toList();
    }
  }
}
