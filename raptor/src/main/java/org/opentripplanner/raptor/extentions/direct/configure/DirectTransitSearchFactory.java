package org.opentripplanner.raptor.extentions.direct.configure;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.extentions.direct.api.RaptorDirectTransitRequest;
import org.opentripplanner.raptor.extentions.direct.service.DirectTransitSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;

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
