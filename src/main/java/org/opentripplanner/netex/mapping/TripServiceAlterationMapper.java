package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.TripAlteration;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public class TripServiceAlterationMapper {
  public static TripAlteration mapAlteration(ServiceAlterationEnumeration netexValue) {
    if (netexValue == null) { return null; }
    switch (netexValue) {
      case PLANNED: return TripAlteration.planned;
      case CANCELLATION: return TripAlteration.cancellation;
      case REPLACED: return TripAlteration.replaced;
      case EXTRA_JOURNEY: return TripAlteration.extraJourney;
    }
    throw new IllegalArgumentException("Unmapped alternation: " + netexValue);
  }
}
