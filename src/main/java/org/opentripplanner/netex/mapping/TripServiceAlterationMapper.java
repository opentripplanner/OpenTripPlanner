package org.opentripplanner.netex.mapping;

import org.opentripplanner.transit.model.timetable.TripAlteration;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public class TripServiceAlterationMapper {

  public static TripAlteration mapAlteration(ServiceAlterationEnumeration netexValue) {
    if (netexValue == null) {
      return TripAlteration.PLANNED;
    }
    return switch (netexValue) {
      case PLANNED -> TripAlteration.PLANNED;
      case CANCELLATION -> TripAlteration.CANCELLATION;
      case REPLACED -> TripAlteration.REPLACED;
      case EXTRA_JOURNEY -> TripAlteration.EXTRA_JOURNEY;
    };
  }
}
