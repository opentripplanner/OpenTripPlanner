package org.opentripplanner.netex.mapping;

import java.util.Optional;
import org.opentripplanner.model.WheelchairAccessibility;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;

public class WheelChairMapper {

  /**
   * If input and containing objects are not null, get the LimitationStatusEnumeration and map to
   * internal {@link WheelchairAccessibility} enumeration.
   *
   * @param accessibilityAssessment NeTEx object wrapping information regarding WheelChairBoarding
   * @param defaultValue            If no {@link AccessibilityAssessment} is defined, default to
   *                                this value
   * @return Mapped enumerator, {@link WheelchairAccessibility#NO_INFORMATION} if no value is found
   */
  public static WheelchairAccessibility wheelchairAccessibility(
    AccessibilityAssessment accessibilityAssessment,
    WheelchairAccessibility defaultValue
  ) {
    if (defaultValue == null) {
      defaultValue = WheelchairAccessibility.NO_INFORMATION;
    }

    return Optional
      .ofNullable(accessibilityAssessment)
      .map(AccessibilityAssessment::getLimitations)
      .map(AccessibilityLimitations_RelStructure::getAccessibilityLimitation)
      .map(AccessibilityLimitation::getWheelchairAccess)
      .map(WheelChairMapper::fromLimitationStatusEnumeration)
      .orElse(defaultValue);
  }

  public static WheelchairAccessibility fromLimitationStatusEnumeration(
    LimitationStatusEnumeration wheelChairLimitation
  ) {
    if (wheelChairLimitation == null) {
      return WheelchairAccessibility.NO_INFORMATION;
    }

    switch (wheelChairLimitation.value()) {
      case "true":
        return WheelchairAccessibility.POSSIBLE;
      case "false":
        return WheelchairAccessibility.NOT_POSSIBLE;
      default:
        return WheelchairAccessibility.NO_INFORMATION;
    }
  }
}
