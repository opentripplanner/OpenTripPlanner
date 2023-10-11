package org.opentripplanner.ext.digitransitemissions;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class EmissionsDataModel implements Serializable {

  private Map<FeedScopedId, Emissions> emissions;
  private double carAvgCo2EmissionsPerMeter;

  @Inject
  public EmissionsDataModel() {}

  public EmissionsDataModel(
    Map<FeedScopedId, Emissions> emissions,
    double carAvgCo2EmissionsPerMeter
  ) {
    this.emissions = emissions;
    this.carAvgCo2EmissionsPerMeter = carAvgCo2EmissionsPerMeter;
  }

  /**
   * For testing only.
   * @param digitransitEmissions
   */
  public void setDigitransitEmissions(
    Map<FeedScopedId, DigitransitEmissions> digitransitEmissions
  ) {
    Map<FeedScopedId, Emissions> emissions = new HashMap<>();
    for (FeedScopedId id : digitransitEmissions.keySet()) {
      emissions.put(id, digitransitEmissions.get(id));
    }
    this.emissions = emissions;
  }

  public void setEmissions(Map<FeedScopedId, Emissions> emissions) {
    this.emissions = emissions;
  }

  public Optional<Map<FeedScopedId, Emissions>> getEmissions() {
    return Optional.ofNullable(this.emissions);
  }

  public void setCarAvgCo2EmissionsPerMeter(double carAvgCo2EmissionsPerMeter) {
    this.carAvgCo2EmissionsPerMeter = carAvgCo2EmissionsPerMeter;
  }

  public Optional<Double> getCarAvgCo2EmissionsPerMeter() {
    return Optional.ofNullable(this.carAvgCo2EmissionsPerMeter);
  }
}
