package org.opentripplanner.api.mapping;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.api.response.RoutingError;

public class PlannerErrorMapper {

    public static PlannerError mapMessage(RoutingError domain) {
        if(domain == null) { return null; }

        PlannerError api = new PlannerError();

        switch (domain.code) {
            case SYSTEM_ERROR:
                api.message = Message.SYSTEM_ERROR;
                break;
            case OUTSIDE_BOUNDS:
                api.message = Message.OUTSIDE_BOUNDS;
                break;
            case OUTSIDE_SERVICE_PERIOD:
                api.message = Message.NO_TRANSIT_TIMES;
                break;
            case LOCATION_NOT_FOUND:
                // TODO Map to correct Message based on location.
                //      What to do in case of intermediate places not found?
                api.message = Message.GEOCODE_FROM_TO_NOT_FOUND;
                break;
            case NO_STOPS_IN_RANGE:
                api.message = Message.LOCATION_NOT_ACCESSIBLE;
                break;
            default:
                throw new IllegalArgumentException();
        }

        api.msg = domain.message;

        return api;
    }
}
