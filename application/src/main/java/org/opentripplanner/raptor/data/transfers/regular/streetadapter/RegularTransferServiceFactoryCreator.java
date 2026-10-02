package org.opentripplanner.raptor.data.transfers.regular.streetadapter;

import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.RegularTransferServiceFactory;
import org.opentripplanner.transit.transfer.regular.internal.DefaultRegularTransferServiceFactory;

/**
 * Creates the request-scoped {@link RegularTransferServiceFactory} for the repository snapshot of a
 * request. Holds the parts of the regular-transfer pipeline that do not change at runtime - the
 * stop index and the street path provider - so they are built once per process, not per request.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public class RegularTransferServiceFactoryCreator {

  private final StopIndex stopIndex;
  private final StreetTransferPathProvider pathProvider;

  private RegularTransferServiceFactoryCreator(
    StopIndex stopIndex,
    StreetTransferPathProvider pathProvider
  ) {
    this.stopIndex = stopIndex;
    this.pathProvider = pathProvider;
  }

  /** The transit model must be indexed, the stop index is created from its stops. */
  public static RegularTransferServiceFactoryCreator of(
    Graph graph,
    TransitRepository transitRepository
  ) {
    var siteRepository = transitRepository.getSiteRepository();
    var stopIndex = new StopIndex(
      siteRepository.stopIndexSize(),
      siteRepository.listRegularStops(),
      RegularStop::getIndex,
      RegularStop::getId
    );
    var nearbyStopFinder = StreetTransferPathProvider.createNearbyStopFinder(
      graph,
      transitRepository
    );
    return new RegularTransferServiceFactoryCreator(
      stopIndex,
      new StreetTransferPathProvider(graph, nearbyStopFinder)
    );
  }

  public RegularTransferServiceFactory<NearbyStop> create(
    RegularTransferRepositorySnapshot<NearbyStop> snapshot
  ) {
    return new DefaultRegularTransferServiceFactory<>(stopIndex, snapshot, pathProvider);
  }
}
