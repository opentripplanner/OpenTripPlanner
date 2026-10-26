package org.opentripplanner.transfer.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.framework.transaction.configure.TransitDomain;
import org.opentripplanner.core.model.transaction.RepositoryHandle;
import org.opentripplanner.core.model.transaction.RepositoryRegistry;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;

/**
 * Wires the regular-transfer repository for routing. The runtime repository is created from the
 * build repository and registered on the transit domain's transaction framework; requests read
 * its snapshot through the returned handle, updaters write through it.
 */
@Module
public class RegularTransferRepositoryModule {

  /**
   * Converts the build repository into the runtime repository, and seals it. From now on regular
   * transfers are only updated through the handle. See
   * {@link StreetTransferFactory#registerRepository}.
   * <p>
   * Registration is not thread-safe, so the handle must be created at startup, before the updaters
   * start - not lazily by the first request. The transit repository must be indexed.
   */
  @Provides
  @Singleton
  public static RepositoryHandle<
    RegularTransferRepositorySnapshot<NearbyStop, RouteRequest>,
    RegularTransferRepository<NearbyStop>
  > regularTransferRepositoryHandle(
    @TransitDomain RepositoryRegistry repositoryRegistry,
    RegularTransferBuildRepository<NearbyStop> buildRepository,
    BuildConfig buildConfig,
    Graph graph,
    TransitRepository transitRepository
  ) {
    return StreetTransferFactory.registerRepository(
      repositoryRegistry,
      buildRepository,
      buildConfig.transferProfiles(),
      graph,
      transitRepository,
      // The statistics are only collected for the report API
      OTPFeature.ReportApi.isOn()
    );
  }
}
