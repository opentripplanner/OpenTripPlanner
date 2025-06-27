package org.opentripplanner.model.impl;

import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
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

  public Optional<TransitMode> findReplacementMode(Trip trip) {
    if (trip.getNetexSubMode() != SubMode.UNKNOWN) {
      Optional<SubmodeMappingRow> mapping = mapNetexSubmode(trip.getNetexSubMode());
      if (mapping.isPresent()) {
        return Optional.of(mapping.get().replacementMode());
      }
    }
    var route = trip.getRoute();
    if (route.getGtfsType() != null) {
      Optional<SubmodeMappingRow> mapping = mapGtfsExtendedType(route.getGtfsType());
      if (mapping.isPresent()) {
        return Optional.of(mapping.get().replacementMode());
      }
    }
    return Optional.empty();
  }
}
