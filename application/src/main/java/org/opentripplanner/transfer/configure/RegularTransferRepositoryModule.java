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
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.report.RegularTransferCacheMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wires the regular-transfer repository for routing. The runtime repository is created from the
 * build repository and registered on the transit domain's transaction framework; requests read
 * its snapshot through the returned handle, updaters write through it.
 */
@Module
public class RegularTransferRepositoryModule {

  private static final Logger LOG = LoggerFactory.getLogger(RegularTransferRepositoryModule.class);

  /**
   * Converts the build repository into the runtime repository, and seals it. From now on regular
   * transfers are only updated through the handle. See
   * {@link StreetTransferFactory#registerRepository}.
   * <p>
   * Registration is not thread-safe, so the handle must be created at startup, before the updaters
   * start - not lazily by the first request. The transit repository must be indexed.
   * <p>
   * The street parameters of the transfers that are different from the {@code routingDefaults} of
   * the router configuration are logged as a warning for each profile type.
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
    RouterConfig routerConfig,
    Graph graph,
    TransitRepository transitRepository
  ) {
    for (var warning : TransferRequestDifferences.warnings(
      buildConfig.transferProfiles(),
      routerConfig.routingRequestDefaults()
    )) {
      LOG.warn(warning);
    }
    return StreetTransferFactory.registerRepository(
      repositoryRegistry,
      buildRepository,
      buildConfig.transferProfiles(),
      graph,
      transitRepository,
      routerConfig.transitTuningConfig().transferCacheMaxSize(),
      OTPFeature.ReportApi.isOn()
    );
  }

  /**
   * The counters of the cache of the Raptor transfers, for the monitoring. They are shared by all
   * snapshots, so the counters of the first snapshot are the ones of every snapshot.
   */
  @Provides
  @Singleton
  public static RegularTransferCacheMetrics regularTransferCacheMetrics(
    @TransitDomain RepositoryRegistry repositoryRegistry,
    RepositoryHandle<
      RegularTransferRepositorySnapshot<NearbyStop, RouteRequest>,
      RegularTransferRepository<NearbyStop>
    > regularTransferRepositoryHandle
  ) {
    return regularTransferRepositoryHandle
      .repositorySnapshot(repositoryRegistry.scope())
      .cacheMetrics();
  }
}
