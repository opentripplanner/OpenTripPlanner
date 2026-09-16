package org.opentripplanner.graph_builder.module.transfer;

import java.util.List;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfilesConfig;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.streetadapter.StreetTransferPathProvider;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.internal.DefaultTransferGenerator;
import org.opentripplanner.transit.transfer.regular.internal.RegularTransferRepository;
import org.opentripplanner.transit.transfer.regular.spi.CostTolerance;
import org.opentripplanner.transit.transfer.regular.spi.RegularTransferParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilderModule} that generates regular (non-constrained) transfers through the
 * {@code raptor-data} pipeline - the primary source of regular transfers for Raptor routing
 * whenever a {@link org.opentripplanner.street.graph.Graph} is available (production graph
 * builds always have one, even without OSM data - see {@code StreetTransferPathProvider}'s
 * straight-line fallback).
 * <p>
 * Runs unconditionally, alongside {@link DirectTransferGenerator}: that module's own
 * {@code PathTransfer} output is still needed for FLEX and as the graph-less fallback
 * ({@code RaptorTransferIndex}) used when {@code RaptorTransitData} is built without this
 * pipeline's wiring (e.g. in tests).
 * <p>
 * {@code regularTransferRepository} is injected empty (mirroring {@code TransferRepository}) and
 * populated in place here, so the same instance can be threaded through
 * {@code SerializedGraphObject} and survive a build-then-load-later-process deployment.
 */
public class RaptorDataTransferGenerator implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(RaptorDataTransferGenerator.class);

  private final Graph graph;
  private final TransitRepository transitRepository;
  private final TransferProfilesConfig config;
  private final RegularTransferRepository<NearbyStop> regularTransferRepository;

  public RaptorDataTransferGenerator(
    Graph graph,
    TransitRepository transitRepository,
    TransferProfilesConfig config,
    RegularTransferRepository<NearbyStop> regularTransferRepository
  ) {
    this.graph = graph;
    this.transitRepository = transitRepository;
    this.config = config;
    this.regularTransferRepository = regularTransferRepository;
  }

  @Override
  public void buildGraph() {
    transitRepository.index();
    var transitService = new DefaultTransitService(transitRepository);
    var siteRepository = transitRepository.getSiteRepository();
    var regularStops = siteRepository.listRegularStops();

    var stopIndex = new StopIndex(
      siteRepository.stopIndexSize(),
      regularStops,
      RegularStop::getIndex,
      RegularStop::getId
    );

    List<FeedScopedId> stopsWithTrips = regularStops
      .stream()
      .filter(stop -> !transitService.findPatterns(stop).isEmpty())
      .map(RegularStop::getId)
      .toList();

    List<RegularTransferParameters<RouteRequest>> profiles = config
      .profiles()
      .stream()
      .map(p ->
        new RegularTransferParameters<>(p.profileId(), p.deduplicationProfile(), p.preferences())
      )
      .toList();

    var nearbyStopFinder = StreetTransferPathProvider.createNearbyStopFinder(
      graph,
      transitRepository
    );
    var pathProvider = new StreetTransferPathProvider(
      graph,
      nearbyStopFinder,
      config,
      transitRepository
    );
    CostTolerance deduplicateTolerance = baseCost ->
      config.deduplicateDelta().calculate(Cost.costOfCentiSeconds(baseCost)).toCentiSeconds();

    new DefaultTransferGenerator<>(
      stopIndex,
      stopsWithTrips,
      pathProvider,
      profiles,
      deduplicateTolerance,
      regularTransferRepository
    ).generateTransfersForAllStops();

    LOG.info(
      "Done generating raptor-data regular transfers for {} profiles, {} stops with trips.",
      profiles.size(),
      stopsWithTrips.size()
    );
  }
}
