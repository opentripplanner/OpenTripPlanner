package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.model.routing.TripSearchMetadata;

import java.util.ArrayList;
import java.util.List;

public class PlanResponse {
    public TripPlan plan;
    public TripSearchMetadata metadata;
    public List<Message> messages = new ArrayList<>();
    public DebugOutput debugOutput = new DebugOutput();


    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("PlanResponse{");
        sb.append("plan=").append(plan);
        sb.append(", metadata=").append(metadata);
        sb.append(", messages=").append(messages);
        sb.append(", debugOutput=").append(debugOutput);
        sb.append('}');
        return sb.toString();
    }
}
