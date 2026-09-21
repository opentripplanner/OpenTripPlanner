package org.opentripplanner.ext.fares.model;

import java.time.LocalTime;
import org.opentripplanner.core.model.id.FeedScopedId;

public class TimeframeBuilder {

  FeedScopedId serviceId;
  LocalTime start = LocalTime.MIN;
  LocalTime end = LocalTime.MAX;

  public TimeframeBuilder withServiceId(FeedScopedId serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  public TimeframeBuilder withStart(LocalTime start) {
    this.start = start;
    return this;
  }

  public TimeframeBuilder withEnd(LocalTime end) {
    this.end = end;
    return this;
  }

  public Timeframe build() {
    return new Timeframe(this);
  }
}
