package org.opentripplanner.raptor.rangeraptor.context;

import java.util.List;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.via.RaptorViaLocation;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

public class SearchContextBuilder<T extends RaptorTripSchedule> {

  private final RaptorRequest<T> request;
  private final RaptorTuningParameters tuningParameters;
  private final RaptorTransitDataProvider<T> transit;

  public SearchContextBuilder(
    RaptorRequest<T> request,
    RaptorTuningParameters tuningParameters,
    RaptorTransitDataProvider<T> transit
  ) {
    this.request = request;
    this.tuningParameters = tuningParameters;
    this.transit = transit;
  }

  public SearchContext<T> build() {
    return new SearchContext<>(
      request,
      tuningParameters,
      transit,
      accessPaths(),
      viaConnections(),
      egressPaths()
    );
  }

  private AccessPaths accessPaths() {
    int iterationStep = tuningParameters.iterationDepartureStepInSeconds();
    boolean forward = request.searchDirection().isForward();
    var params = request.searchParams();
    var paths = forward ? params.accessPaths() : params.egressPaths();
    return AccessPaths.create(iterationStep, paths, request.profile(), request.searchDirection());
  }

  private List<ViaConnections> viaConnections() {
    return request
      .searchParams()
      .viaLocations()
      .stream()
      .map(RaptorViaLocation::connections)
      .map(ViaConnections::new)
      .toList();
  }

  private EgressPaths egressPaths() {
    boolean forward = request.searchDirection().isForward();
    var params = request.searchParams();
    var paths = forward ? params.egressPaths() : params.accessPaths();
    return EgressPaths.create(paths, request.profile());
  }
}
