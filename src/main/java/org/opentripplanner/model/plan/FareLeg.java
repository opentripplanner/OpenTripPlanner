package org.opentripplanner.model.plan;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.transit.model.site.FareZone;

public interface FareLeg extends Leg {
  default Set<FareZone> getFareZones() {
    var intermediate = getIntermediateStops()
      .stream()
      .flatMap(stopArrival -> stopArrival.place.stop.getFareZones().stream());

    var start = this.getFrom().stop.getFareZones().stream();
    var end = this.getTo().stop.getFareZones().stream();

    return Stream.of(intermediate, start, end).flatMap(s -> s).collect(Collectors.toSet());
  }
}
