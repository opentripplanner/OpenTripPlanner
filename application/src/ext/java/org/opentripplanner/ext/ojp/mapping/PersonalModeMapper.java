package org.opentripplanner.ext.ojp.mapping;

import de.vdv.ojp20.PersonalModesEnumeration;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.TraverseMode;

class PersonalModeMapper {

  public static StreetMode toStreetMode(PersonalModesEnumeration x) {
    return switch (x) {
      case FOOT -> StreetMode.WALK;
      case BICYCLE -> StreetMode.BIKE;
      case SCOOTER -> StreetMode.SCOOTER_RENTAL;
      case CAR -> StreetMode.CAR;
      case MOTORCYCLE, TRUCK, OTHER -> throw new IllegalArgumentException("Unsupported mode: " + x);
    };
  }

  static PersonalModesEnumeration mapToOjp(TraverseMode mode) {
    return switch (mode) {
      case WALK -> PersonalModesEnumeration.FOOT;
      case BICYCLE -> PersonalModesEnumeration.BICYCLE;
      case SCOOTER -> PersonalModesEnumeration.SCOOTER;
      case CAR, FLEX -> PersonalModesEnumeration.CAR;
    };
  }
}
