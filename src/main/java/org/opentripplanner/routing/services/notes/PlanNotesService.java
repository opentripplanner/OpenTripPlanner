package org.opentripplanner.routing.services.notes;

import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.RoutingRequest;

import java.util.Collection;

public interface PlanNotesService {
    Collection<Alert> getAlerts(RoutingRequest request, TripPlan plan);
}
