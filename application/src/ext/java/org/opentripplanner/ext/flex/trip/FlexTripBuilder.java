package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

public abstract class FlexTripBuilder<T extends FlexTrip<T, B>, B extends FlexTripBuilder<T, B>>
  extends AbstractEntityBuilder<T, B> {

  private Trip trip;

  FlexTripBuilder(FeedScopedId id) {
    super(id);
  }

  FlexTripBuilder(T original) {
    super(original);
    this.trip = original.getTrip();
  }

  abstract B instance();

  public B withTrip(Trip trip) {
    this.trip = trip;
    return instance();
  }

  public Trip trip() {
    return trip;
  }
}
