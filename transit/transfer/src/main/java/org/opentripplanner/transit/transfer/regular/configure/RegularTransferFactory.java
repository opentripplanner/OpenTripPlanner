package org.opentripplanner.transit.transfer.regular.configure;

import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.raptor.data.stop.RaptorStopIndex;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferGenerator;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.internal.DefaultRegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.internal.DefaultRegularTransferGenerator;
import org.opentripplanner.transit.transfer.regular.internal.ProfileUserPreferences;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepositoryLifecycle;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

/**
 * Creates the implementations of the regular-transfer component, so code outside the component
 * never references its {@code internal} classes. This module has no dependency injection; the
 * application wires these into its own modules, with the concrete path type.
 */
public final class RegularTransferFactory {

  private RegularTransferFactory() {}

  /**
   * Check the profiles that can not be checked when the parameters are created: the user
   * preferences of the profiles must be unique, and a profile without preferences gets them from
   * its request with the {@code mapper}. Call it when the configuration is read, to fail before
   * the graph build starts.
   *
   * @throws IllegalArgumentException if two profiles have equal user preferences
   */
  public static <R> void validate(
    TransferParameters<R> parameters,
    TransferRequestMapper<R> mapper
  ) {
    ProfileUserPreferences.resolveAll(parameters, mapper);
  }

  /** Create an empty build repository, populated at graph build. */
  public static <P> RegularTransferBuildRepository<P> createBuildRepository() {
    return new DefaultRegularTransferBuildRepository<>();
  }

  /**
   * Create the graph-build generator, writing the transfers of all stops into the repository.
   *
   * @param <P> the street path type
   * @param <R> the request type
   */
  public static <P, R> RegularTransferGenerator createTransferGenerator(
    RaptorStopIndex stopIndex,
    TransferParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    RegularTransferBuildRepository<P> buildRepository
  ) {
    return new DefaultRegularTransferGenerator<>(
      stopIndex,
      parameters,
      pathProvider,
      mapper,
      buildRepository
    );
  }

  /**
   * Register the runtime repository on the transaction framework, created from the paths of the
   * build repository. This seals the build repository. The snapshots use the {@code pathProvider}
   * to derive the Raptor transfers from their paths, with the request of the profile the paths are
   * stored under, and the {@code mapper} to map the routing requests to user preferences.
   *
   * @param numberOfStops the size of the stop index space ({@code RaptorStopIndex.size()}), not the
   *                      number of regular stops. The index space is shared with the flex stops,
   *                      which do not take part in Raptor, so the Raptor transfer data is larger
   *                      than needed. This goes away when the stop index moves into Raptor.
   * @param cacheMaxSize  the max number of Raptor transfers cached for other user preferences than
   *                      the ones of the profiles and the warmup entries, which are always cached
   */
  public static <P, R> RepositoryHandle<
    RegularTransferRepositorySnapshot<P, R>,
    RegularTransferRepository<P>
  > registerRepository(
    RepositoryRegistry repositoryRegistry,
    RegularTransferBuildRepository<P> buildRepository,
    TransferParameters<R> parameters,
    TransferPathProvider<P, R> pathProvider,
    TransferRequestMapper<R> mapper,
    int numberOfStops,
    int cacheMaxSize,
    boolean collectCacheStatistics
  ) {
    var handle = repositoryRegistry.registerRepository(
      buildRepository.createRuntimeRepository(),
      new RegularTransferRepositoryLifecycle<>(
        pathProvider,
        mapper,
        parameters,
        numberOfStops,
        cacheMaxSize,
        collectCacheStatistics
      )
    );
    // Create the Raptor transfers of the warmup entries before the first request
    handle.repositorySnapshot(repositoryRegistry.scope()).warmUp();
    return handle;
  }
}
