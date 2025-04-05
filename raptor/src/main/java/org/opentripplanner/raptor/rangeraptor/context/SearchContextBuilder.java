package org.opentripplanner.raptor.rangeraptor.context;

import java.util.List;
import java.util.function.IntPredicate;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.rangeraptor.transit.AccessPaths;
import org.opentripplanner.raptor.rangeraptor.transit.EgressPaths;
import org.opentripplanner.raptor.rangeraptor.transit.ViaConnections;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;

public class SearchContextBuilder<T extends RaptorTripSchedule> {

  private final RaptorRequest<T> request;
  private final RaptorTuningParameters tuningParameters;
  private final RaptorTransitDataProvider<T> transit;

  @Nullable
  private final IntPredicate acceptC2AtDestination;

  public SearchContextBuilder(
    RaptorRequest<T> request,
    RaptorTuningParameters tuningParameters,
    RaptorTransitDataProvider<T> transit,
    @Nullable IntPredicate acceptC2AtDestination
  ) {
    this.request = request;
    this.tuningParameters = tuningParameters;
    this.transit = transit;
    this.acceptC2AtDestination = acceptC2AtDestination;
  }

  public SearchContext<T> build() {
    return new SearchContext<>(
      request,
      tuningParameters,
      transit,
      accessPaths(),
      viaConnections(),
      egressPaths(),
      acceptC2AtDestination
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
    // TODO VIA - This need to be changed if we allow mixing visit-via and pass-through
    return request.searchParams().isVisitViaSearch()
      ? request
        .searchParams()
        .viaLocations()
        .stream()
        .map(RaptorViaLocation::connections)
        .map(ViaConnections::new)
        .toList()
      : List.of();
  }

  private EgressPaths egressPaths() {
    boolean forward = request.searchDirection().isForward();
    var params = request.searchParams();
    var paths = forward ? params.egressPaths() : params.accessPaths();
    return EgressPaths.create(paths, request.profile());
  }
}
