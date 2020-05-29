package org.opentripplanner.ext.transmodelapi.model;

import org.opentripplanner.api.common.Message;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.util.ResourceBundleSingleton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PlanResponse {
    public TripPlan plan;
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

    public List<String> listErrorMessages(String locale) {
        return messages
                .stream()
                .map(message -> message.get(ResourceBundleSingleton.INSTANCE.getLocale(locale)))
                .collect(Collectors.toList());
    }
}
