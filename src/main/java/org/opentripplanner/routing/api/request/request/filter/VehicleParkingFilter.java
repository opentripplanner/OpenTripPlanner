package org.opentripplanner.routing.api.request.request.filter;

import com.google.common.collect.Sets;
import java.util.Set;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;

public sealed interface VehicleParkingFilter {
  boolean matches(VehicleParking p);
  int size();

  record TagsFilter(Set<String> tags) implements VehicleParkingFilter {
    @Override
    public boolean matches(VehicleParking p) {
      return tags.isEmpty() || !Sets.intersection(tags, p.getTags()).isEmpty();
    }

    @Override
    public int size() {
      return tags.size();
    }

    @Override
    public String toString() {
      return "tags=" + tags.stream().sorted().toList();
    }
  }
}
