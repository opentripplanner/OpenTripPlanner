package org.opentripplanner.transit.service;

import java.util.List;
import org.opentripplanner.transit.model.site.StopLocation;

public class SiteRepositoryMock extends SiteRepository {

  private final List<StopLocation> stops;

  public SiteRepositoryMock(List<StopLocation> stops) {
    this.stops = stops;
  }

  @Override
  public StopLocation stopByIndex(int index) {
    return stops.get(index);
  }

  @Override
  public int stopIndexSize() {
    return stops.size();
  }
}
