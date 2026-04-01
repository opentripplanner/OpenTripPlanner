package org.opentripplanner.transit.model.network;

import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

public class ReplacementForRelation {

  private final TripOnServiceDate tripOnServiceDate;

  public ReplacementForRelation(TripOnServiceDate tripOnServiceDate) {
    this.tripOnServiceDate = tripOnServiceDate;
  }

  public TripOnServiceDate getTripOnServiceDate() {
    return tripOnServiceDate;
  }
}
