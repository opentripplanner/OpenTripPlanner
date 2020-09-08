package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import java.util.Collection;

/**
 * This class represents the different variations of what is considered flexible transit, and its
 * subclasses encapsulates the different business logic, which the different types of services
 * adhere to.
 */
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
