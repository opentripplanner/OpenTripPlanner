package org.opentripplanner.api.mapping;

import org.opentripplanner.model.WheelChairBoarding;

public class WheelchairBoardingMapper {
    public static Integer mapToApi(WheelChairBoarding domain) {
        return domain == null
                ? WheelChairBoarding.NO_INFORMATION.gtfsCode
                : domain.gtfsCode;
    }
}
