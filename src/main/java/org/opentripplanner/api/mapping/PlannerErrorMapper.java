package org.opentripplanner.api.mapping;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;

public class PlannerErrorMapper {

    public static PlannerError mapMessage(RoutingError domain) {
        if(domain == null) { return null; }

        PlannerError api = new PlannerError();

        switch (domain.code) {
            case NO_TRANSIT_CONNECTION:
                api.message = Message.PATH_NOT_FOUND;
                break;
            case OUTSIDE_BOUNDS:
                api.message = Message.OUTSIDE_BOUNDS;
                break;
            case OUTSIDE_SERVICE_PERIOD:
                api.message = Message.NO_TRANSIT_TIMES;
                break;
            case LOCATION_NOT_FOUND:
                if (domain.inputField.equals(InputField.FROM_PLACE)) {
                    api.message = Message.GEOCODE_FROM_NOT_FOUND;
                } else if (domain.inputField.equals(InputField.TO_PLACE)) {
                    api.message = Message.GEOCODE_TO_NOT_FOUND;
                } else {
                    throw new IllegalArgumentException();
                }
                break;
            case NO_STOPS_IN_RANGE:
                api.message = Message.LOCATION_NOT_ACCESSIBLE;
                break;
            default:
                throw new IllegalArgumentException();
        }

        return api;
    }
}
