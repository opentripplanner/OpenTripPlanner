package org.opentripplanner.api.mapping;

import org.opentripplanner.model.WheelchairBoarding;

public class WheelchairBoardingMapper {

  public static Integer mapToApi(WheelchairBoarding domain) {
    return domain == null ? WheelchairBoarding.NO_INFORMATION.gtfsCode : domain.gtfsCode;
  }
}
