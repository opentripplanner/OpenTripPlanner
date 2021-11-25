package org.opentripplanner.api.mapping;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.error.PlannerError;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;

import java.util.List;

public class PlannerErrorMapper {

    public static PlannerError mapMessage(RoutingError domain) {
        if(domain == null) { return null; }

        PlannerError api;

        switch (domain.code) {
            case NO_TRANSIT_CONNECTION:
            case NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW:
                api = new PlannerError(Message.PATH_NOT_FOUND);
                break;
            case OUTSIDE_BOUNDS:
                api = new PlannerError(Message.OUTSIDE_BOUNDS);
                api.setMissing(List.of(domain.inputField.name()));
                break;
            case OUTSIDE_SERVICE_PERIOD:
                api = new PlannerError(Message.NO_TRANSIT_TIMES);
                break;
            case LOCATION_NOT_FOUND:
                if (domain.inputField.equals(InputField.FROM_PLACE)) {
                    api = new PlannerError(Message.GEOCODE_FROM_NOT_FOUND);
                    api.setMissing(List.of(domain.inputField.name()));
                } else if (domain.inputField.equals(InputField.TO_PLACE)) {
                    api = new PlannerError(Message.GEOCODE_TO_NOT_FOUND);
                    api.setMissing(List.of(domain.inputField.name()));
                } else {
                    throw new IllegalArgumentException();
                }
                break;
            case NO_STOPS_IN_RANGE:
                api = new PlannerError(Message.LOCATION_NOT_ACCESSIBLE);
                api.setMissing(List.of(domain.inputField.name()));
                break;
            case WALKING_BETTER_THAN_TRANSIT:
                api = new PlannerError(Message.TOO_CLOSE);
                break;
            case SYSTEM_ERROR:
                api = new PlannerError(Message.SYSTEM_ERROR);
                break;
            default:
                throw new IllegalArgumentException();
        }

        return api;
    }
}
