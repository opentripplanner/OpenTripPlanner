package org.opentripplanner.transit.model.network.grouppriority;

import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.Trip;

class TripAdapter implements EntityAdapter {

  private final Trip trip;

  public TripAdapter(Trip trip) {
    this.trip = trip;
  }

  @Override
  public TransitMode mode() {
    return trip.getMode();
  }

  @Override
  public String subMode() {
    return trip.getNetexSubMode().name();
  }

  @Override
  public FeedScopedId agencyId() {
    return trip.getRoute().getAgency().getId();
  }

  @Override
  public FeedScopedId routeId() {
    return trip.getRoute().getId();
  }
}
