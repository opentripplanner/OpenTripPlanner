package org.opentripplanner.ext.emissions;

import java.util.Optional;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@Sandbox
public interface EmissionsService {
  Optional<Double> getEmissionsPerMeterForRoute(
    FeedScopedId feedScopedRouteId,
    EmissionType emissionType
  );

  Optional<Double> getCarEmissionsPerMeter(EmissionType emissionType);
}
