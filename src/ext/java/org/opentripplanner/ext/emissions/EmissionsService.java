package org.opentripplanner.ext.emissions;

import java.io.Serializable;
import java.util.HashMap;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.model.plan.Itinerary;

@Sandbox
public interface EmissionsService extends Serializable {
  HashMap<String, DigitransitEmissionsAgency> getEmissionByAgency();

  Float getEmissionsForRoute(Itinerary itinerary);
}
