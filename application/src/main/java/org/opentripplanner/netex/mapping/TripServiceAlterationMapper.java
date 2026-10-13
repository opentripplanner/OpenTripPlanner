package org.opentripplanner.netex.mapping;

import org.entur.netex2.model.ServiceAlterationEnumeration;
import org.opentripplanner.transit.model.timetable.TripAlteration;

public class TripServiceAlterationMapper {

  public static TripAlteration mapAlteration(ServiceAlterationEnumeration netexValue) {
    if (netexValue == null) {
      return TripAlteration.PLANNED;
    }
    return switch (netexValue) {
      case PLANNED, PROVISIONAL -> TripAlteration.PLANNED;
      case CANCELLATION -> TripAlteration.CANCELLATION;
      case REPLACED -> TripAlteration.REPLACED;
      case EXTRA_JOURNEY -> TripAlteration.EXTRA_JOURNEY;
    };
  }
}
