package org.opentripplanner.transfer.configure;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.dataimport.StopsVisitedByPatterns;
import org.opentripplanner.transfer.streetadapter.RaptorStopIndexAdaptor;
import org.opentripplanner.transfer.streetadapter.RegularTransferPreferencesMapper;
import org.opentripplanner.transfer.streetadapter.StreetTransferPathProvider;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferGenerator;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;
import org.opentripplanner.transit.transfer.regular.spi.AllowedModesLookup;

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
  public static TransferParameters<RouteRequest> defaultParameters() {
    return new TransferParameters<>(
      List.of(
        new TransferProfile<>("walk", TransferProfileType.WALK, null, RouteRequest.defaultValue())
      )
    );
  }

  /**
   * Create the graph-build generator, writing the transfers of all stops into
   * {@code buildRepository}.
   *
   * @param stopPatterns the trip patterns visiting the stops, only the stops visited by a pattern
   *                     have transfers from them, and with the feature
   *                     {@code ConsiderPatternsForDirectTransfers} only the closest stop of each
   *                     pattern is a transfer to
   * @param allowedModes the modes the stops are allowed for, to select the max duration of the
   *                     transfers, see {@code MaxDurationsPreference}
   * @param flexIndex    the flex trips to consider when the transfer stops are found, or
   *                     {@code null} to ignore flex trips
   */
  public static RegularTransferGenerator createGenerator(
    Graph graph,
    TransitRepository transitRepository,
    TransferParameters<RouteRequest> parameters,
    RegularTransferBuildRepository<NearbyStop> buildRepository,
    StopsVisitedByPatterns stopPatterns,
    AllowedModesLookup allowedModes,
    @Nullable FlexIndex flexIndex
  ) {
    return RegularTransferFactory.createTransferGenerator(
      new RaptorStopIndexAdaptor(transitRepository.getSiteRepository()),
      parameters,
      new StreetTransferPathProvider(
        graph,
        StreetTransferPathProvider.createNearbyStopFinder(
          graph,
          transitRepository,
          stopPatterns,
          flexIndex
        ),
        stopPatterns::contains,
        allowedModes
      ),
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
    TransferParameters<RouteRequest> parameters,
    Graph graph,
    TransitRepository transitRepository
  ) {
    return RegularTransferFactory.registerRepository(
      repositoryRegistry,
      buildRepository,
      parameters,
      createPathProvider(graph, transitRepository),
      new RegularTransferPreferencesMapper(),
      transitRepository.getSiteRepository().stopIndexSize()
    );
  }

  /** The routing path provider only re-costs paths, it does not search for transfers. */
  private static StreetTransferPathProvider createPathProvider(
    Graph graph,
    TransitRepository transitRepository
  ) {
    return new StreetTransferPathProvider(
      graph,
      StreetTransferPathProvider.createNearbyStopFinder(graph, transitRepository)
    );
  }
}
