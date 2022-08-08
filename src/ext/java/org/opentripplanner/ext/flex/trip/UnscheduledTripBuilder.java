package org.opentripplanner.ext.flex.trip;

import java.util.List;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class UnscheduledTripBuilder
  extends FlexTripBuilder<UnscheduledTrip, UnscheduledTripBuilder> {

  private List<StopTime> stopTimes;

  UnscheduledTripBuilder(FeedScopedId id) {
    super(id);
  }

  public UnscheduledTripBuilder(UnscheduledTrip original) {
    super(original);
    throw new IllegalStateException("Updating is not supported for UnscheduledTrip");
    // TODO: implement copying
    // this.stopTimes = original.stopTimes;
  }

  public UnscheduledTripBuilder withStopTimes(List<StopTime> stopTimes) {
    this.stopTimes = stopTimes;
    return this;
  }

  public List<StopTime> stopTimes() {
    return stopTimes;
  }

  @Override
  UnscheduledTripBuilder instance() {
    return this;
  }

  @Override
  protected UnscheduledTrip buildFromValues() {
    return new UnscheduledTrip(this);
  }
}
