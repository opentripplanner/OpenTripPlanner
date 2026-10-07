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
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;

/**
 * Wires the regular-transfer pipeline for routing. The runtime repository is created from the build
 * repository and registered on the transit domain's transaction framework; requests read its
 * snapshot through the returned handle, updaters write through it. The request-scoped factory is
 * provided by {@code RequestScopedModule}.
 * <p>
 * Both bindings are resolved eagerly at startup by {@code ConstructApplication}.
 */
@Module
public class RegularTransferRepositoryModule {

  /**
   * Converts the build repository into the initial snapshot of the runtime repository, and seals it. From now on
   * regular transfers are only updated through the handle.
   * <p>
   * Registration is not thread-safe, so the handle must be created at startup, before the updaters start - not lazily
   * by the first request. {@code ConstructApplication} does this.
   */
  @Provides
  @Singleton
  public static RepositoryHandle<
    RegularTransferRepositorySnapshot<NearbyStop>,
    RegularTransferRepository<NearbyStop>
  > regularTransferRepositoryHandle(
    @TransitDomain RepositoryRegistry repositoryRegistry,
    RegularTransferBuildRepository<NearbyStop> buildRepository
  ) {
    return RegularTransferFactory.registerRepository(repositoryRegistry, buildRepository);
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
