package org.opentripplanner.ext.digitransitemissions;

import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Itinerary;

@Sandbox
public interface EmissionsService {
  Double getEmissionsForItinerary(Itinerary itinerary);
}
