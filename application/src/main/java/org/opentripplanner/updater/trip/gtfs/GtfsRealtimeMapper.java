package org.opentripplanner.updater.trip.gtfs;

import com.google.transit.realtime.GtfsRealtime;
import java.util.Optional;
import org.opentripplanner.transit.model.basic.Accessibility;

class GtfsRealtimeMapper {

  public static Optional<Accessibility> mapWheelchairAccessible(
    GtfsRealtime.VehicleDescriptor.WheelchairAccessible wheelchairAccessible
  ) {
    return Optional.ofNullable(
      switch (wheelchairAccessible) {
        case WHEELCHAIR_ACCESSIBLE -> Accessibility.POSSIBLE;
        case WHEELCHAIR_INACCESSIBLE -> Accessibility.NOT_POSSIBLE;
        case UNKNOWN -> Accessibility.NO_INFORMATION;
        default -> null;
      }
    );
  }
}
