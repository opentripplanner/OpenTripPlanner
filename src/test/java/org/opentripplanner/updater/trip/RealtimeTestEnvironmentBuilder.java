package org.opentripplanner.updater.trip;

import java.util.Objects;

public class RealtimeTestEnvironmentBuilder {

  private RealtimeTestEnvironment.SourceType sourceType;
  private boolean withTrip1 = false;
  private boolean withTrip2 = false;

  RealtimeTestEnvironmentBuilder withSourceType(RealtimeTestEnvironment.SourceType sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  public RealtimeTestEnvironmentBuilder withTrip1() {
    withTrip1 = true;
    return this;
  }

  public RealtimeTestEnvironmentBuilder withTrip2() {
    withTrip2 = true;
    return this;
  }

  public RealtimeTestEnvironment build() {
    Objects.requireNonNull(sourceType, "sourceType cannot be null");
    return new RealtimeTestEnvironment(sourceType, withTrip1, withTrip2);
  }
}
