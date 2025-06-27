package org.opentripplanner.model.impl;

import java.util.Map;
import java.util.Optional;
import org.opentripplanner.model.FeedType;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.timetable.Trip;

public class SubmodeMappingService {

  private final Map<SubmodeMappingMatcher, SubmodeMappingRow> map;

  public SubmodeMappingService(Map<SubmodeMappingMatcher, SubmodeMappingRow> map) {
    this.map = map;
  }

  public Optional<SubmodeMappingRow> mapGtfsExtendedType(int extendedType) {
    return Optional.ofNullable(
      map.get(new SubmodeMappingMatcher(FeedType.GTFS, Integer.toString(extendedType)))
    );
  }

  public Optional<SubmodeMappingRow> mapNetexSubmode(SubMode submode) {
    return Optional.ofNullable(
      map.get(new SubmodeMappingMatcher(FeedType.NETEX, submode.toString()))
    );
  }

  // For replacement services in NeTEx feeds, trip.mode is the original mode and
  // trip.netexSubMode implies the replacement mode.
  // For replacement services in GTFS feeds, route.mode is the replacement mode
  // and route.type implies the original mode.
  // We allow overriding, but if the mapping file fails to specify, we return the logical choice.
  public TransitMode findOriginalMode(Trip trip) {
    var route = trip.getRoute();
    if (trip.getNetexSubMode() == SubMode.UNKNOWN && route.getGtfsType() != null) {
      Optional<SubmodeMappingRow> mapping = mapGtfsExtendedType(route.getGtfsType());
      if (mapping.isPresent() && mapping.get().originalMode() != null) {
        return mapping.get().originalMode();
      }
    }
    return trip.getMode();
  }

  public Optional<TransitMode> findReplacementMode(Trip trip) {
    if (trip.getNetexSubMode() != SubMode.UNKNOWN) {
      Optional<SubmodeMappingRow> mapping = mapNetexSubmode(trip.getNetexSubMode());
      if (mapping.isPresent()) {
        return Optional.ofNullable(mapping.get().replacementMode());
      }
    }
    var route = trip.getRoute();
    if (route.getGtfsType() != null) {
      Optional<SubmodeMappingRow> mapping = mapGtfsExtendedType(route.getGtfsType());
      if (mapping.isPresent()) {
        if (mapping.get().replacementMode() != null) {
          return Optional.of(mapping.get().replacementMode());
        } else {
          return Optional.of(route.getMode());
        }
      }
    }
    return Optional.empty();
  }
}
