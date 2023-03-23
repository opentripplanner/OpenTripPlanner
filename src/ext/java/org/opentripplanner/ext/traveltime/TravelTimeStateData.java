package org.opentripplanner.ext.traveltime;

import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.search.state.StateData;

public class TravelTimeStateData extends StateData {

  protected final long postTransitDepartureTime;

  public TravelTimeStateData(StreetMode streetMode, long postTransitDepartureTime) {
    super(streetMode);
    this.postTransitDepartureTime = postTransitDepartureTime;
  }
}
