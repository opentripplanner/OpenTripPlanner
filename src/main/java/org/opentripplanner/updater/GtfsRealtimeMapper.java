package org.opentripplanner.updater;

import com.google.transit.realtime.GtfsRealtime;
import java.util.Optional;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;

public class GtfsRealtimeMapper {

  public static Optional<WheelchairAccessibility> mapWheelchairAccessible(
    GtfsRealtime.VehicleDescriptor.WheelchairAccessible wheelchairAccessible
  ) {
    return Optional.ofNullable(
      switch (wheelchairAccessible) {
        case WHEELCHAIR_ACCESSIBLE -> WheelchairAccessibility.POSSIBLE;
        case WHEELCHAIR_INACCESSIBLE -> WheelchairAccessibility.NOT_POSSIBLE;
        case UNKNOWN -> WheelchairAccessibility.NO_INFORMATION;
        default -> null;
      }
    );
  }
}
