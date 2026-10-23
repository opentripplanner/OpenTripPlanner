package org.opentripplanner.transfer.dataimport;

import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.transfer.dataimport.api.TransferProfilesConfig;
import org.opentripplanner.transfer.streetadapter.RaptorStopIndexAdaptor;
import org.opentripplanner.transfer.streetadapter.StreetTransferPathProvider;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.transit.transfer.regular.RegularTransferBuildRepository;
import org.opentripplanner.transit.transfer.regular.TransferGenerator;
import org.opentripplanner.transit.transfer.regular.configure.RegularTransferFactory;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;

/**
 * {@link GraphBuilderModule} that generates regular (non-constrained) transfers through the
 * regular-transfer module - the primary source of regular transfers for Raptor routing whenever a
 * {@link org.opentripplanner.street.graph.Graph} is available (production graph builds always
 * have one, even without OSM data - see {@code StreetTransferPathProvider}'s straight-line
 * fallback).
 * <p>
 * Runs unconditionally, alongside {@link FlexTransferGenerator}: that module now only
 * generates the FLEX-relevant transfers (walking connectors between {@code RegularStop}s and
 * flex {@code AreaStop}/{@code GroupStop}s) - regular transfers between {@code RegularStop}s are
 * exclusively this pipeline's responsibility.
 * <p>
 * The injected {@link TransferGenerator} populates the {@code RegularTransferBuildRepository} in
 * place, so the same instance can be threaded through {@code SerializedGraphObject} and survive a
 * build-then-load-later-process deployment. At runtime it is converted into the initial snapshot of
 * the transactional repository.
 */
public class RegularTransitTransferGenerator implements GraphBuilderModule {

  private final TransferGenerator transferGenerator;
  private final Graph graph;
  private final TransitRepository transitRepository;
  private final StopsVisitedByPatterns stopsVisitedByPatterns;
  private final DataImportIssueStore issueStore;

  public RegularTransitTransferGenerator(
    TransferGenerator transferGenerator,
    Graph graph,
    TransitRepository transitRepository,
    StopsVisitedByPatterns stopsVisitedByPatterns,
    DataImportIssueStore issueStore
  ) {
    this.transferGenerator = transferGenerator;
    this.graph = graph;
    this.transitRepository = transitRepository;
    this.stopsVisitedByPatterns = stopsVisitedByPatterns;
    this.issueStore = issueStore;
  }

  @Override
  public void buildGraph() {
    transitRepository.index();
    transferGenerator.generateTransfersForAllStops();
    reportStopsWithoutTransfers();
  }

  private void reportStopsWithoutTransfers() {
    Set<FeedScopedId> stopsWithTransfers = transferGenerator.listAllStopsUsedInTransfers();
    for (RegularStop stop : transitRepository.getSiteRepository().listRegularStops()) {
      var stopId = stop.getId();
      if (stopsVisitedByPatterns.contains(stopId) && !stopsWithTransfers.contains(stopId)) {
        var stopVertex = graph.getStopVertex(stopId);
        if (stopVertex != null) {
          issueStore.add(new StopNotLinkedForTransfers(stopVertex));
        }
      }
    }
  }

  public static TransferGenerator create(
    Graph graph,
    TransitRepository transitRepository,
    TransferProfilesConfig config,
    RegularTransferBuildRepository<NearbyStop> buildRepository,
    StopsVisitedByPatterns stopsVisitedByPatterns
  ) {
    var nearbyStopFinder = StreetTransferPathProvider.createNearbyStopFinder(
      graph,
      transitRepository
    );
    var pathProvider = new StreetTransferPathProvider(
      graph,
      nearbyStopFinder,
      stopsVisitedByPatterns::contains
    );

    var profileList = config
      .profiles()
      .stream()
      .map(p -> new TransferProfile<>("NOT IN USE", p.userPreferences()))
      .toList();
    TransferProfiles<?> profiles = TransferProfiles.of(profileList);

    return RegularTransferFactory.createTransferGenerator(
      new RaptorStopIndexAdaptor(transitRepository.getSiteRepository()),
      pathProvider,
      profiles,
      buildRepository
    );
  }
}
