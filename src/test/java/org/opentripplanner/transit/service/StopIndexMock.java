package org.opentripplanner.transit.service;

import java.util.List;
import org.opentripplanner.transit.model.site.StopLocation;

public class StopIndexMock extends StopModelIndex {

  private final List<StopLocation> stops;

  public StopIndexMock(List<StopLocation> stops) {
    super(new StopModel());
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
