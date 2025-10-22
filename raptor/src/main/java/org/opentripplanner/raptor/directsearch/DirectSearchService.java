package org.opentripplanner.raptor.directsearch;

import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;

public class DirectSearchService {
  private final RaptorTransitDataProvider data;

  public DirectSearchService(RaptorTransitDataProvider data) {
    this.data = data;
  }

}
