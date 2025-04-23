package org.opentripplanner.ext.emission.internal;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultEmissionRepository implements EmissionRepository {

  private Emission carAvgCo2PerMeter = Emission.ZERO;
  private Map<FeedScopedId, Emission> routeEmissions = new HashMap<>();
  private Map<FeedScopedId, TripPatternEmission> tripEmissions = new HashMap<>();

  public DefaultEmissionRepository() {}

  @Override
  public void setCarAvgCo2PerMeter(Gram carAvgCo2PerMeter) {
    this.carAvgCo2PerMeter = Emission.of(carAvgCo2PerMeter);
  }

  @Override
  public Emission carAvgPassengerEmissionPerMeter() {
    return this.carAvgCo2PerMeter;
  }

  @Override
  public void addRouteEmissions(Map<FeedScopedId, Emission> routeAvgCo2Emissions) {
    this.routeEmissions.putAll(routeAvgCo2Emissions);
  }

  @Override
  public Emission routePassengerEmissionsPerMeter(FeedScopedId routeId) {
    var value = this.routeEmissions.get(routeId);
    return value == null ? Emission.ZERO : value;
  }

  @Override
  public TripPatternEmission tripPatternEmissions(FeedScopedId tripId) {
    return tripEmissions.get(tripId);
  }

  @Override
  public void addTripPatternEmissions(Map<FeedScopedId, TripPatternEmission> tripPatternEmissions) {
    this.tripEmissions.putAll(tripPatternEmissions);
  }
}
