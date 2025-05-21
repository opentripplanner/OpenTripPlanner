package org.opentripplanner.ext.emission.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.model.EmissionSummary;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultEmissionRepository implements EmissionRepository {

  private Emission carAvgCo2PerMeter = Emission.ZERO;
  private Map<FeedScopedId, Emission> emissionForRouteId = new HashMap<>();
  private Map<FeedScopedId, TripPatternEmission> emissionForTripId = new HashMap<>();

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
    this.emissionForRouteId.putAll(routeAvgCo2Emissions);
  }

  @Override
  public Optional<Emission> routePassengerEmissionsPerMeter(FeedScopedId routeId) {
    return Optional.ofNullable(this.emissionForRouteId.get(routeId));
  }

  @Override
  public TripPatternEmission tripPatternEmissions(FeedScopedId tripId) {
    return emissionForTripId.get(tripId);
  }

  @Override
  public void addTripPatternEmissions(Map<FeedScopedId, TripPatternEmission> tripPatternEmissions) {
    this.emissionForTripId.putAll(tripPatternEmissions);
  }

  @Override
  public EmissionSummary summary() {
    return new EmissionSummary(
      emissionForRouteId.keySet().size(),
      emissionForTripId.keySet().size()
    );
  }
}
