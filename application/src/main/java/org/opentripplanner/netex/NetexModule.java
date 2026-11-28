package org.opentripplanner.netex;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToGraph;
import org.opentripplanner.graph_builder.module.AddTransitEntitiesToTimetable;
import org.opentripplanner.graph_builder.module.TransitWithFutureDateValidator;
import org.opentripplanner.model.TransitDataImport;
import org.opentripplanner.model.impl.TransitDataImportBuilder;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VehicleParkingHelper;
import org.opentripplanner.transit.dataimport.TimetableDataImport;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.service.TransitRepository;

/**
 * This module is used for importing the NeTEx CEN Technical Standard for exchanging Public
 * Transport schedules and related data (<a href="http://netex-cen.eu/">http://netex-cen.eu/</a>).
 * Currently it only supports the Norwegian profile (<a href=
 * "https://enturas.atlassian.net/wiki/spaces/PUBLIC/">https://enturas.atlassian.net/wiki/spaces/PUBLIC/</a>),
 * but it is intended to be updated later to support other profiles.
 */
public class NetexModule implements GraphBuilderModule {

  private final int subwayAccessTime;

  private final Graph graph;
  private final DeduplicatorService deduplicator;
  private final TransitRepository transitRepository;
  private final TimetableDataImport timetableDataImport;
  private final VehicleParkingRepository parkingRepository;
  private final StreetDetailsRepository streetDetailsRepository;
  private final DataImportIssueStore issueStore;

  /**
   * This collection is a queue because the bundles contain state that remains after loading. They
   * should be garbage collected after loading is finished.
   */
  private final Queue<NetexBundle> netexBundles;

  public NetexModule(
    Graph graph,
    DeduplicatorService deduplicator,
    TransitRepository transitRepository,
    TimetableDataImport timetableDataImport,
    VehicleParkingRepository parkingRepository,
    StreetDetailsRepository streetDetailsRepository,
    DataImportIssueStore issueStore,
    int subwayAccessTime,
    List<NetexBundle> netexBundles
  ) {
    this.graph = graph;
    this.deduplicator = deduplicator;
    this.transitRepository = transitRepository;
    this.timetableDataImport = timetableDataImport;
    this.parkingRepository = parkingRepository;
    this.streetDetailsRepository = streetDetailsRepository;
    this.issueStore = issueStore;
    this.subwayAccessTime = subwayAccessTime;
    this.netexBundles = new ArrayDeque<>(netexBundles);
  }

  @Override
  public void buildGraph() {
    try {
      TripCalendars tripCalendars = TripCalendars.empty();

      while (!netexBundles.isEmpty()) {
        TransitDataImportBuilder transitBuilder = loadNextBundle();

        TransitDataImport otpService = transitBuilder.build();

        AddTransitEntitiesToTimetable.addToTimetable(
          otpService,
          transitRepository,
          timetableDataImport
        );
        AddTransitEntitiesToGraph.addToGraph(
          otpService,
          subwayAccessTime,
          graph,
          streetDetailsRepository
        );

        var lots = transitBuilder.vehicleParkings();
        parkingRepository.updateVehicleParking(lots, List.of());
        var linker = new VehicleParkingHelper(graph);
        lots.forEach(linker::linkVehicleParkingToGraph);

        tripCalendars = tripCalendars.merge(otpService.getTripCalendars());
      }

      timetableDataImport.mergeTripCalendars(tripCalendars);

      TransitWithFutureDateValidator.validate(
        tripCalendars,
        issueStore,
        transitRepository.getTimeZone()
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Load the next bundle and close its data source. The NeTEx module is the only reader of the
   * NeTEx data sources, so the source is released as soon as the bundle is loaded - a streamed zip
   * file keeps its entire uncompressed content in memory until it is closed. The data sources are
   * closed again at the end of the graph build by their owner, which has no effect.
   */
  private TransitDataImportBuilder loadNextBundle() throws IOException {
    try (var netexBundle = netexBundles.poll()) {
      netexBundle.checkInputs();
      return netexBundle.loadBundle(deduplicator, issueStore);
    }
  }

  @Override
  public void checkInputs() {
    netexBundles.forEach(NetexBundle::checkInputs);
  }
}
