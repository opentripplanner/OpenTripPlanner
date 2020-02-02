package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.model.routing.TripSearchMetadata;

import java.util.ArrayList;
import java.util.List;

public class PlanResponse {
    public ApiTripPlan plan;
    public TripSearchMetadata metadata;
    public List<Message> messages = new ArrayList<>();
    public DebugOutput debugOutput = new DebugOutput();


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
