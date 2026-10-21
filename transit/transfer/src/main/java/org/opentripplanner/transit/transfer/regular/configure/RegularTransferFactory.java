package org.opentripplanner.transit.transfer.regular.configure;

import java.util.Collection;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.TransferGenerator;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.internal.DefaultRegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.internal.DefaultTransferGenerator;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepositoryLifecycle;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;
import org.opentripplanner.transit.transfer.regular.spi.TransferPathProvider;

/**
 * Creates the implementations of the regular-transfer component, so code outside the component
 * never references its {@code internal} classes. This module has no dependency injection; the
 * application wires these into its own modules, with the concrete path type.
 */
public final class RegularTransferFactory {

  private RegularTransferFactory() {}

  /** Create an empty build repository, populated at graph build. */
  public static <P> RegularTransferBuildRepository<P> createBuildRepository() {
    return new DefaultRegularTransferBuildRepository<>();
  }

  /** Create the graph-build generator, writing the transfers of all stops into the repository. */
  public static <P, U> TransferGenerator createTransferGenerator(
    StopIndex stopIndex,
    Collection<FeedScopedId> stopsWithTrips,
    TransferPathProvider<P, U> pathProvider,
    TransferProfiles<U> profiles,
    RegularTransferBuildRepository<P> buildRepository
  ) {
    return new DefaultTransferGenerator<>(
      stopIndex,
      stopsWithTrips,
      pathProvider,
      profiles,
      buildRepository
    );
  }

  /**
   * Register the runtime repository on the transaction framework, created from the paths of the
   * build repository. This seals the build repository. The snapshots use the {@code stopIndex} and
   * the {@code pathProvider} to derive the Raptor transfers from their paths.
   */
  public static <P> RepositoryHandle<
    RegularTransferRepositorySnapshot<P>,
    RegularTransferRepository<P>
  > registerRepository(
    RepositoryRegistry repositoryRegistry,
    RegularTransferBuildRepository<P> buildRepository,
    StopIndex stopIndex,
    TransferPathProvider<P, AbstractUserPreferences<?>> pathProvider
  ) {
    return repositoryRegistry.registerRepository(
      buildRepository.createRuntimeRepository(),
      new RegularTransferRepositoryLifecycle<>(stopIndex, pathProvider)
    );
  }
}
