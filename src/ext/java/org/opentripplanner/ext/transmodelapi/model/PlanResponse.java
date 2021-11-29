package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.model.plan.PageCursor;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.TripSearchMetadata;

import java.util.ArrayList;
import java.util.List;

public class PlanResponse {
    public TripPlan plan;
    public TripSearchMetadata metadata;
    public List<RoutingError> messages = new ArrayList<>();
    public DebugOutput debugOutput;
    public PageCursor pageCursor;


    @Override
    public String toString() {
        return  "PlanResponse{"
                + "plan=" + plan
                + ", metadata=" + metadata
                + ", messages=" + messages
                + ", debugOutput=" + debugOutput
                + '}';
    }
}
