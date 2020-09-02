package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import java.util.Collection;
public abstract class FlexTrip extends TransitEntity<FeedScopedId> {

  protected final Trip trip;

  public FlexTrip(Trip trip) {
    this.trip = trip;
  }

  public abstract Collection<StopLocation> getStops();

  @Override
  public FeedScopedId getId() {
    return trip.getId();
  }

  public Trip getTrip() {
    return trip;
  }
}
