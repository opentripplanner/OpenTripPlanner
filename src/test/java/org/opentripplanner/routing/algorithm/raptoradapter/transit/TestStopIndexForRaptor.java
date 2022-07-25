package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.List;
import org.opentripplanner.transit.model.site.StopLocation;

public class TestStopIndexForRaptor implements StopIndexForRaptor {

  private final List<StopLocation> stops;

  public TestStopIndexForRaptor(List<StopLocation> stops) {
    this.stops = stops;
  }

  @Override
  public StopLocation stopByIndex(int index) {
    return stops.get(index);
  }

  @Override
  public int indexOf(StopLocation stop) {
    return stops.indexOf(stop);
  }

  @Override
  public int size() {
    return stops.size();
  }
}
