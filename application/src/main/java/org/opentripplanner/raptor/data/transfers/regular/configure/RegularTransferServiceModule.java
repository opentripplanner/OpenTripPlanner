package org.opentripplanner.raptor.data.transfers.regular.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.framework.transaction.configure.TransitDomain;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.transfers.regular.streetadapter.RegularTransferServiceFactoryCreator;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepositoryLifecycle;

/**
 * Wires the regular-transfer pipeline for routing. The repository built (or loaded) with the graph
 * is registered on the transit domain's transaction framework; requests read its snapshot through
 * the returned handle, updaters write through it. The request-scoped factory is provided by
 * {@code RequestScopedModule}.
 */
@Module
public class RegularTransferServiceModule {

  /**
   * Freezes the repository into the initial snapshot. The repository must not be written to
   * directly after this - write through the handle instead.
   * <p>
   * Registration is not thread-safe, so the handle must be created at startup, before the
   * updaters start - not lazily by the first request. {@code ConstructApplication} does this.
   */
  @Provides
  @Singleton
  public static RepositoryHandle<
    RegularTransferRepositorySnapshot<NearbyStop>,
    RegularTransferRepository<NearbyStop>
  > regularTransferRepositoryHandle(
    @TransitDomain RepositoryRegistry repositoryRegistry,
    RegularTransferRepository<NearbyStop> repository
  ) {
    return repositoryRegistry.registerRepository(
      repository,
      new RegularTransferRepositoryLifecycle<>()
    );
  }

  @Provides
  @Singleton
  public static RegularTransferServiceFactoryCreator regularTransferServiceFactoryCreator(
    Graph graph,
    TransitRepository transitRepository
  ) {
    return RegularTransferServiceFactoryCreator.of(graph, transitRepository);
  }
}
