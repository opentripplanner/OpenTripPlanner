package org.opentripplanner.api.adapters;

import org.opentripplanner.model.WheelChairBoarding;

public class WheelChairCodeMapper {
        static int mapToWheelChairCode(WheelChairBoarding wheelChairBoarding) {
                switch (wheelChairBoarding) {
                case NOT_POSSIBLE:
                        return 2;
                case POSSIBLE:
                        return 1;
                case NO_INFORMATION:
                default:
                        return 0;
                }
        }

        static WheelChairBoarding mapFromWheelChairCode(int wheelChairCode) {
                switch (wheelChairCode) {
                case 2:
                        return WheelChairBoarding.NOT_POSSIBLE;
                case 1:
                        return WheelChairBoarding.POSSIBLE;
                case 0:
                default:
                        return WheelChairBoarding.NO_INFORMATION;
                }
        }
}
