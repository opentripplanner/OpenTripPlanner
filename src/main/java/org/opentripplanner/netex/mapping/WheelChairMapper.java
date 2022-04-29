package org.opentripplanner.netex.mapping;

import java.util.Optional;
import org.opentripplanner.model.WheelchairBoarding;
import org.rutebanken.netex.model.AccessibilityAssessment;
import org.rutebanken.netex.model.AccessibilityLimitation;
import org.rutebanken.netex.model.AccessibilityLimitations_RelStructure;
import org.rutebanken.netex.model.LimitationStatusEnumeration;

public class WheelChairMapper {

  /**
   * If input and containing objects are not null, get the LimitationStatusEnumeration and map to
   * internal {@link WheelchairBoarding} enumeration.
   *
   * @param accessibilityAssessment NeTEx object wrapping information regarding WheelChairBoarding
   * @param defaultValue            If no {@link AccessibilityAssessment} is defined, default to
   *                                this value
   * @return Mapped enumerator, {@link WheelchairBoarding#NO_INFORMATION} if no value is found
   */
  public static WheelchairBoarding wheelChairBoarding(
    AccessibilityAssessment accessibilityAssessment,
    WheelchairBoarding defaultValue
  ) {
    if (defaultValue == null) {
      defaultValue = WheelchairBoarding.NO_INFORMATION;
    }

    return Optional
      .ofNullable(accessibilityAssessment)
      .map(AccessibilityAssessment::getLimitations)
      .map(AccessibilityLimitations_RelStructure::getAccessibilityLimitation)
      .map(AccessibilityLimitation::getWheelchairAccess)
      .map(WheelChairMapper::fromLimitationStatusEnumeration)
      .orElse(defaultValue);
  }

  public static WheelchairBoarding fromLimitationStatusEnumeration(
    LimitationStatusEnumeration wheelChairLimitation
  ) {
    if (wheelChairLimitation == null) {
      return WheelchairBoarding.NO_INFORMATION;
    }

    switch (wheelChairLimitation.value()) {
      case "true":
        return WheelchairBoarding.POSSIBLE;
      case "false":
        return WheelchairBoarding.NOT_POSSIBLE;
      default:
        return WheelchairBoarding.NO_INFORMATION;
    }
  }
}
