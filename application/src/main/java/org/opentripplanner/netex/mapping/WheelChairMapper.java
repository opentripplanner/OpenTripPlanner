package org.opentripplanner.netex.mapping;

import java.util.Optional;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;

public class WheelChairMapper {

  /**
   * If input and containing objects are not null, get the LimitationStatusEnumeration and map to
   * internal {@link Accessibility} enumeration.
   *
   * @param accessibilityAssessment NeTEx object wrapping information regarding WheelChairBoarding
   * @param defaultValue            If no {@link AccessibilityAssessment} is defined, default to
   *                                this value
   * @return Mapped enumerator, {@link Accessibility#NO_INFORMATION} if no value is found
   */
  public static Accessibility wheelchairAccessibility(
    AccessibilityAssessment accessibilityAssessment,
    Accessibility defaultValue
  ) {
    if (defaultValue == null) {
      defaultValue = Accessibility.NO_INFORMATION;
    }

    return Optional.ofNullable(accessibilityAssessment)
      .map(AccessibilityAssessment::getLimitations)
      .map(AccessibilityLimitations_RelStructure::getAccessibilityLimitation)
      .map(AccessibilityLimitation::getWheelchairAccess)
      .map(WheelChairMapper::fromLimitationStatusEnumeration)
      .orElse(defaultValue);
  }

  public static Accessibility fromLimitationStatusEnumeration(
    LimitationStatusEnumeration wheelChairLimitation
  ) {
    if (wheelChairLimitation == null) {
      return Accessibility.NO_INFORMATION;
    }

    switch (wheelChairLimitation.value()) {
      case "true":
        return Accessibility.POSSIBLE;
      case "false":
        return Accessibility.NOT_POSSIBLE;
      default:
        return Accessibility.NO_INFORMATION;
    }
  }
}
