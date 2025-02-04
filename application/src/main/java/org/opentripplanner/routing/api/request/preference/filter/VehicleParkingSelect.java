package org.opentripplanner.routing.api.request.preference.filter;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;

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

  /**
   * Get the tags from the select. Is not meant for checking for matches.
   */
  List<String> tags();

  final class TagsSelect implements VehicleParkingSelect {

    private final Set<String> tags;

    public TagsSelect(Set<String> tags) {
      this.tags = tags;
    }

    public List<String> tags() {
      return tags.stream().toList();
    }

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
