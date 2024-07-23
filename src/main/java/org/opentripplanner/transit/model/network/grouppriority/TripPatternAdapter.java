package org.opentripplanner.transit.model.network.grouppriority;

import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;

class TripPatternAdapter implements EntityAdapter {

  private final TripPattern tripPattern;

  public TripPatternAdapter(TripPattern tripPattern) {
    this.tripPattern = tripPattern;
  }

  @Override
  public TransitMode mode() {
    return tripPattern.getMode();
  }

  @Override
  public String subMode() {
    return tripPattern.getNetexSubmode().name();
  }

  @Override
  public FeedScopedId agencyId() {
    return tripPattern.getRoute().getAgency().getId();
  }

  @Override
  public FeedScopedId routeId() {
    return tripPattern.getRoute().getId();
  }
}
