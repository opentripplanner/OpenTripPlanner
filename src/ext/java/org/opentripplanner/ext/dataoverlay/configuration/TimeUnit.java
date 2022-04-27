package org.opentripplanner.ext.dataoverlay.configuration;

import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

public enum TimeUnit {
  MS_EPOCH(ChronoUnit.MILLIS),
  SECONDS(ChronoUnit.SECONDS),
  HOURS(ChronoUnit.HOURS);

  private final ChronoUnit chronoUnit;

  TimeUnit(ChronoUnit chronoUnit) {
    this.chronoUnit = chronoUnit;
  }

  public int between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
    return (int) asChronoUnit().between(temporal1Inclusive, temporal2Exclusive);
  }

  private ChronoUnit asChronoUnit() {
    return chronoUnit;
  }
}
