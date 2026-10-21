package org.opentripplanner.graph_builder.module.transfer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.transfer.api.TransferProfilesConfig;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.data.stop.StopIndex;
import org.opentripplanner.raptor.data.transfers.regular.streetadapter.StreetTransferPathProvider;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.api.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilderModule} that generates regular (non-constrained) transfers through the
 * {@code raptor-data} pipeline - the primary source of regular transfers for Raptor routing
 * whenever a {@link org.opentripplanner.street.graph.Graph} is available (production graph
 * builds always have one, even without OSM data - see {@code StreetTransferPathProvider}'s
 * straight-line fallback).
 * <p>
 * Runs unconditionally, alongside {@link FlexTransferGenerator}: that module now only
 * generates the FLEX-relevant transfers (walking connectors between {@code RegularStop}s and
 * flex {@code AreaStop}/{@code GroupStop}s) - regular transfers between {@code RegularStop}s are
 * exclusively this pipeline's responsibility.
 * <p>
 * {@code regularTransferBuildRepository} is injected empty (mirroring {@code TransferRepository})
 * and populated in place here, so the same instance can be threaded through
 * {@code SerializedGraphObject} and survive a build-then-load-later-process deployment. At runtime
 * it is converted into the initial snapshot of the transactional repository.
 */
public class RegularTransitTransferGenerator implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(RegularTransitTransferGenerator.class);

  private final Graph graph;
  private final TransitRepository transitRepository;
  private final TimetableDataImport timetableDataImport;
  private final TransferProfilesConfig config;
  private final RegularTransferBuildRepository<NearbyStop> regularTransferBuildRepository;
  private final DataImportIssueStore issueStore;

  public RegularTransitTransferGenerator(
    Graph graph,
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport,
    TransferProfilesConfig config,
    RegularTransferBuildRepository<NearbyStop> regularTransferBuildRepository,
    DataImportIssueStore issueStore
  ) {
    this.graph = graph;
    this.transitRepository = transitRepository;
    this.timetableDataImport = timetableDataImport;
    this.config = config;
    this.regularTransferBuildRepository = regularTransferBuildRepository;
    this.issueStore = issueStore;
  }

  @Override
  public void buildGraph() {
    transitRepository.index();
    var siteRepository = transitRepository.getSiteRepository();
    var regularStops = siteRepository.listRegularStops();

    var stopIndex = new StopIndex(
      siteRepository.stopIndexSize(),
      regularStops,
      RegularStop::getIndex,
      RegularStop::getId
    );

    var stopsVisitedByPatterns = stopsVisitedByPatterns();
    List<FeedScopedId> stopsWithTrips = regularStops
      .stream()
      .filter(stopsVisitedByPatterns::contains)
      .map(RegularStop::getId)
      .toList();

    List<TransferProfile<AbstractUserPreferences<?>>> profileList = config
      .profiles()
      .stream()
      .map(p ->
        new TransferProfile<AbstractUserPreferences<?>>(p.userPreferences(), p.userPreferences())
      )
      .toList();
    TransferProfiles<AbstractUserPreferences<?>> profiles = TransferProfiles.<
      AbstractUserPreferences<?>
    >of(profileList);

    var nearbyStopFinder = StreetTransferPathProvider.createNearbyStopFinder(
      graph,
      transitRepository
    );
    var pathProvider = new StreetTransferPathProvider(graph, nearbyStopFinder);

    RegularTransferFactory.createTransferGenerator(
      stopIndex,
      stopsWithTrips,
      pathProvider,
      profiles,
      regularTransferBuildRepository
    ).generateTransfersForAllStops();

    for (FeedScopedId stopId : stopsWithTrips) {
      if (!regularTransferBuildRepository.hasTransfersFrom(stopIndex.toStopIndex(stopId))) {
        var stopVertex = graph.getStopVertex(stopId);
        if (stopVertex != null) {
          issueStore.add(new StopNotLinkedForTransfers(stopVertex));
        }
      }
    }
    logSummary(profileList, stopsWithTrips);
  }

  private void logSummary(
    List<TransferProfile<AbstractUserPreferences<?>>> profileList,
    List<FeedScopedId> stopsWithTrips
  ) {
    int sum = 0;
    for (var profile : profileList) {
      int size = regularTransferBuildRepository.calculateNumberOfTransferPaths(
        profile.userPreferences()
      );
      LOG.info("Created {} regular transfers for profile {}.", size, profile.userPreferences());
      sum += size;
    }
    LOG.info(
      "Done generating regular transfers. paths: {}, profiles: {} stops: {}",
      sum,
      profileList.size(),
      stopsWithTrips.size()
    );
  }

  /** The stops visited by at least one scheduled trip pattern. */
  private Set<StopLocation> stopsVisitedByPatterns() {
    return timetableDataImport
      .getAllTripPatterns()
      .stream()
      .flatMap(pattern -> pattern.getStops().stream())
      .collect(Collectors.toSet());
  }
}
