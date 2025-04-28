package org.opentripplanner.graph_builder;

import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.datastore.api.FileType.NETEX;
import static org.opentripplanner.datastore.api.FileType.OSM;

import jakarta.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.configure.DaggerGraphBuilderFactory;
import org.opentripplanner.graph_builder.module.configure.GraphBuilderFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.utils.lang.OtpNumberFormat;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This makes a Graph out of various inputs like GTFS and OSM. It is modular: GraphBuilderModules
 * are placed in a list and run in sequence.
 */
public class GraphBuilder implements Runnable {

  private static final Logger LOG = LoggerFactory.getLogger(GraphBuilder.class);

  private final List<GraphBuilderModule> graphBuilderModules = new ArrayList<>();
  private final Graph graph;
  private final TimetableRepository timetableRepository;
  private final DataImportIssueStore issueStore;

  private boolean hasTransitData = false;

  @Inject
  public GraphBuilder(
    Graph baseGraph,
    TimetableRepository timetableRepository,
    DataImportIssueStore issueStore
  ) {
    this.graph = baseGraph;
    this.timetableRepository = timetableRepository;
    this.issueStore = issueStore;
  }

  /**
   * Factory method to create and configure a GraphBuilder with all the appropriate modules to build
   * a graph from the given data source and configuration directory.
   */
  public static GraphBuilder create(
    BuildConfig config,
    GraphBuilderDataSources dataSources,
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    TimetableRepository timetableRepository,
    WorldEnvelopeRepository worldEnvelopeRepository,
    VehicleParkingRepository vehicleParkingService,
    @Nullable EmissionRepository emissionRepository,
    @Nullable StopConsolidationRepository stopConsolidationRepository,
    StreetLimitationParameters streetLimitationParameters,
    boolean loadStreetGraph,
    boolean saveStreetGraph
  ) {
    boolean hasOsm = dataSources.has(OSM);
    boolean hasGtfs = dataSources.has(GTFS);
    boolean hasNetex = dataSources.has(NETEX);
    boolean hasTransitData = hasGtfs || hasNetex;

    timetableRepository.initTimeZone(config.transitModelTimeZone);

    GraphBuilderFactory.Builder builder = DaggerGraphBuilderFactory.builder();
    builder
      .config(config)
      .graph(graph)
      .osmInfoGraphBuildRepository(osmInfoGraphBuildRepository)
      .timetableRepository(timetableRepository)
      .worldEnvelopeRepository(worldEnvelopeRepository)
      .vehicleParkingRepository(vehicleParkingService)
      .stopConsolidationRepository(stopConsolidationRepository)
      .emissionRepository(emissionRepository)
      .streetLimitationParameters(streetLimitationParameters)
      .dataSources(dataSources)
      .timeZoneId(timetableRepository.getTimeZone());

    var factory = builder.build();

    var graphBuilder = factory.graphBuilder();

    graphBuilder.hasTransitData = hasTransitData;

    if (hasOsm) {
      graphBuilder.addModule(factory.osmModule());
    }

    if (hasGtfs) {
      graphBuilder.addModule(factory.gtfsModule());
    }

    if (hasNetex) {
      graphBuilder.addModule(factory.netexModule());
    }

    // Consolidate stops only if a stop consolidation repo has been provided
    if (hasTransitData) {
      graphBuilder.addModuleOptional(factory.stopConsolidationModule());
      graphBuilder.addModule(factory.tripPatternNamer());
      graphBuilder.addModuleOptional(
        factory.timeZoneAdjusterModule(),
        timetableRepository.getAgencyTimeZones().size() > 1
      );

      if (hasOsm || graphBuilder.graph.hasStreets) {
        graphBuilder.addModule(factory.osmBoardingLocationsModule());
      }
    }

    // This module is outside the hasGTFS conditional block because it also links things like parking
    // which need to be handled even when there's no transit.
    graphBuilder.addModule(factory.streetLinkerModule());

    // Prune graph connectivity islands after transit stop linking, so that pruning can take into account
    // existence of stops in islands. If an island has a stop, it actually may be a real island and should
    // not be removed quite as easily
    if ((hasOsm && !saveStreetGraph) || loadStreetGraph) {
      graphBuilder.addModule(factory.pruneIslands());
    }

    // Load elevation data and apply it to the streets.
    // We want to do run this module after loading the OSM street network but before finding transfers.
    for (GraphBuilderModule it : factory.elevationModules()) {
      graphBuilder.addModule(it);
    }

    if (hasTransitData) {
      // Add links to flex areas after the streets has been split, so that also the split edges are connected
      graphBuilder.addModuleOptional(factory.areaStopsToVerticesMapper(), OTPFeature.FlexRouting);

      // This module will use streets or straight line distance depending on whether OSM data is found in the graph.
      graphBuilder.addModule(factory.directTransferGenerator());

      // Analyze routing between stops to generate report
      graphBuilder.addModuleOptional(factory.directTransferAnalyzer(), OTPFeature.TransferAnalyzer);

      graphBuilder.addModuleOptional(factory.emissionGraphBuilder(), OTPFeature.Emission);
    }

    if (loadStreetGraph || hasOsm) {
      graphBuilder.addModule(factory.graphCoherencyCheckerModule());
    }

    graphBuilder.addModuleOptional(factory.routeToCentroidStationIdValidator());

    graphBuilder.addModuleOptional(factory.dataImportIssueReporter(), config.dataImportReport);

    graphBuilder.addModuleOptional(factory.dataOverlayFactory(), OTPFeature.DataOverlay);

    graphBuilder.addModule(factory.calculateWorldEnvelopeModule());

    return graphBuilder;
  }

  public void run() {
    // Record how long it takes to build the graph, purely for informational purposes.
    long startTime = System.currentTimeMillis();

    // Check all graph builder inputs, and fail fast to avoid waiting until the build process
    // advances.
    for (GraphBuilderModule builder : graphBuilderModules) {
      builder.checkInputs();
    }

    for (GraphBuilderModule load : graphBuilderModules) {
      load.buildGraph();
    }

    new DataImportIssueSummary(issueStore.listIssues()).logSummary();

    // Log before we validate, this way we have more information if the validation fails
    logGraphBuilderCompleteStatus(startTime, graph, timetableRepository);

    validate();
  }

  private void addModuleOptional(@Nullable GraphBuilderModule module, OTPFeature feature) {
    addModuleOptional(module, feature.isOn());
  }

  private void addModuleOptional(@Nullable GraphBuilderModule module, boolean enabled) {
    if (enabled) {
      addModuleOptional(module);
    }
  }

  private void addModuleOptional(@Nullable GraphBuilderModule module) {
    if (module != null) {
      addModule(module);
    }
  }

  private void addModule(GraphBuilderModule module) {
    graphBuilderModules.add(Objects.requireNonNull(module));
  }

  private boolean hasTransitData() {
    return hasTransitData;
  }

  public DataImportIssueSummary issueSummary() {
    return new DataImportIssueSummary(issueStore.listIssues());
  }

  /**
   * Validates the build. Currently, only checks if the graph has transit data if any transit data
   * sets were included in the build. If all transit data gets filtered out due to transit period
   * configuration, for example, then this function will throw a {@link OtpAppException}.
   */
  private void validate() {
    if (hasTransitData() && !timetableRepository.hasTransit()) {
      throw new OtpAppException(
        "The provided transit data have no trips within the configured transit service period. " +
        "There is something wrong with your data - see the log above. Another possibility is that the " +
        "'transitServiceStart' and 'transitServiceEnd' are not correctly configured."
      );
    }
  }

  private static void logGraphBuilderCompleteStatus(
    long startTime,
    Graph graph,
    TimetableRepository timetableRepository
  ) {
    long endTime = System.currentTimeMillis();
    String time = DurationUtils.durationToStr(Duration.ofMillis(endTime - startTime));
    var f = new OtpNumberFormat();
    var nStops = f.formatNumber(timetableRepository.getSiteRepository().stopIndexSize());
    var nPatterns = f.formatNumber(timetableRepository.getAllTripPatterns().size());
    var nTransfers = f.formatNumber(timetableRepository.getTransferService().listAll().size());
    var nVertices = f.formatNumber(graph.countVertices());
    var nEdges = f.formatNumber(graph.countEdges());

    LOG.info("Graph building took {}.", time);
    LOG.info("Graph built.   |V|={} |E|={}", nVertices, nEdges);
    LOG.info(
      "Transit built. |Stops|={} |Patterns|={} |ConstrainedTransfers|={}",
      nStops,
      nPatterns,
      nTransfers
    );
  }
}
