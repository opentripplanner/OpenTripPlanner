package org.opentripplanner.raptor.extensions.direct.configure;

import org.opentripplanner.raptor.extensions.direct.api.RaptorDirectTransitRequest;
import org.opentripplanner.raptor.extensions.direct.service.DirectTransitSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public class DirectTransitSearchFactory {

  public static <T extends RaptorTripSchedule> DirectTransitSearch<T> createSearch(
    RaptorDirectTransitRequest request,
    RaptorTransitDataProvider<T> data
  ) {
    return new DirectTransitSearch<T>(
      request.earliestDepartureTime(),
      request.searchWindowInSeconds(),
      request.relaxC1(),
      request.accessPaths(),
      request.egressPaths(),
      data
    );
  }
}
