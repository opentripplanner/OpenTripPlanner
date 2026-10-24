package org.opentripplanner.transfer.configure;

import java.util.List;
import java.util.function.Predicate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.streetadapter.RaptorStopIndexAdaptor;
import org.opentripplanner.transfer.streetadapter.RegularTransferPreferencesMapper;
import org.opentripplanner.transfer.streetadapter.StreetTransferPathProvider;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.TransferGenerator;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

/**
 * Connects the regular-transfer module to the street graph: creates the module's generator and
 * repository with a street-search-backed {@link StreetTransferPathProvider} and the
 * {@link RegularTransferPreferencesMapper}, with {@code P = NearbyStop} and
 * {@code R = RouteRequest}. This is the only place those are wired together, so the graph build
 * and the routing use the same mapping.
 */
public final class StreetTransferFactory {

  private StreetTransferFactory() {}

  /** The profiles used if none are configured: a single default walk profile. */
  public static TransferProfilesParameters<RouteRequest> defaultParameters() {
    return new TransferProfilesParameters<>(
      List.of(new WalkProfileParameters<>("walk", null, RouteRequest.defaultValue()))
    );
  }

  /**
   * Create the graph-build generator, writing the transfers of all stops into
   * {@code buildRepository}.
   *
   * @param isTransferSource only the stops accepted by this predicate have transfers from them
   */
  public static TransferGenerator createGenerator(
    Graph graph,
    TransitRepository transitRepository,
    TransferProfilesParameters<RouteRequest> parameters,
    RegularTransferBuildRepository<NearbyStop> buildRepository,
    Predicate<FeedScopedId> isTransferSource
  ) {
    return RegularTransferFactory.createTransferGenerator(
      new RaptorStopIndexAdaptor(transitRepository.getSiteRepository()),
      parameters,
      createPathProvider(graph, transitRepository, isTransferSource),
      new RegularTransferPreferencesMapper(),
      buildRepository
    );
  }

  /**
   * Convert the build repository into the runtime repository, register it on the transaction
   * framework, and seal the build repository. The snapshots derive the Raptor transfers from their
   * paths with the path provider created here, which does not search - only re-costs paths.
   * <p>
   * Registration is not thread-safe, so the handle must be created at startup, before the updaters
   * start. The transit repository must be indexed.
   */
  public static RepositoryHandle<
    RegularTransferRepositorySnapshot<NearbyStop, RouteRequest>,
    RegularTransferRepository<NearbyStop>
  > registerRepository(
    RepositoryRegistry repositoryRegistry,
    RegularTransferBuildRepository<NearbyStop> buildRepository,
    TransferProfilesParameters<RouteRequest> parameters,
    Graph graph,
    TransitRepository transitRepository
  ) {
    return RegularTransferFactory.registerRepository(
      repositoryRegistry,
      buildRepository,
      parameters,
      createPathProvider(graph, transitRepository, stopId -> true),
      new RegularTransferPreferencesMapper(),
      transitRepository.getSiteRepository().stopIndexSize()
    );
  }

  private static StreetTransferPathProvider createPathProvider(
    Graph graph,
    TransitRepository transitRepository,
    Predicate<FeedScopedId> isTransferSource
  ) {
    return new StreetTransferPathProvider(
      graph,
      StreetTransferPathProvider.createNearbyStopFinder(graph, transitRepository),
      isTransferSource
    );
  }
}
