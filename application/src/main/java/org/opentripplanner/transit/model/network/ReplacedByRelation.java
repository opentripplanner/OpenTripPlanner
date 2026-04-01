package org.opentripplanner.transit.model.network;

import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

public class ReplacedByRelation {

  private final TripOnServiceDate tripOnServiceDate;

  public ReplacedByRelation(TripOnServiceDate tripOnServiceDate) {
    this.tripOnServiceDate = tripOnServiceDate;
  }

  public TripOnServiceDate getTripOnServiceDate() {
    return tripOnServiceDate;
  }
}
