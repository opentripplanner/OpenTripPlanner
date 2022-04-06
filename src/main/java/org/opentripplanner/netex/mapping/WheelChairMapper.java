package org.opentripplanner.netex.mapping;

import java.util.Optional;
import org.opentripplanner.model.WheelChairBoarding;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;

public class WheelChairMapper {

  /**
   * If input and containing objects are not null, get the LimitationStatusEnumeration and map to
   * internal {@link WheelChairBoarding} enumeration.
   *
   * @param accessibilityAssessment NeTEx object wrapping information regarding
   *                                WheelChairBoarding
   * @param defaultValue            If no {@link AccessibilityAssessment} is defined, default to
   *                                this value
   * @return Mapped enumerator, {@link WheelChairBoarding#NO_INFORMATION} if no value is found
   */
  public static WheelChairBoarding wheelChairBoarding(
    AccessibilityAssessment accessibilityAssessment,
    WheelChairBoarding defaultValue
  ) {
    if (defaultValue == null) {
      defaultValue = WheelChairBoarding.NO_INFORMATION;
    }

    return Optional
      .ofNullable(accessibilityAssessment)
      .map(AccessibilityAssessment::getLimitations)
      .map(AccessibilityLimitations_RelStructure::getAccessibilityLimitation)
      .map(AccessibilityLimitation::getWheelchairAccess)
      .map(WheelChairMapper::fromLimitationStatusEnumeration)
      .orElse(defaultValue);
  }

  public static WheelChairBoarding fromLimitationStatusEnumeration(
    LimitationStatusEnumeration wheelChairLimitation
  ) {
    if (wheelChairLimitation == null) {
      return WheelChairBoarding.NO_INFORMATION;
    }

    switch (wheelChairLimitation.value()) {
      case "true":
        return WheelChairBoarding.POSSIBLE;
      case "false":
        return WheelChairBoarding.NOT_POSSIBLE;
      default:
        return WheelChairBoarding.NO_INFORMATION;
    }
  }
}
