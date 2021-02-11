package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.TripAlteration;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;

public class TripServiceAlterationMapper {
  public static TripAlteration mapAlteration(ServiceAlterationEnumeration netexValue) {
    if (netexValue == null) { return null; }
    switch (netexValue) {
      case PLANNED: return TripAlteration.PLANNED;
      case CANCELLATION: return TripAlteration.CANCELLATION;
      case REPLACED: return TripAlteration.REPLACED;
      case EXTRA_JOURNEY: return TripAlteration.EXTRA_JOURNEY;
    }
    throw new IllegalArgumentException("Unmapped alternation: " + netexValue);
  }
}
