package org.opentripplanner.graph_builder;

import static org.opentripplanner.datastore.api.FileType.GTFS;
import static org.opentripplanner.datastore.api.FileType.NETEX;
import static org.opentripplanner.datastore.api.FileType.OSM;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.framework.lang.OtpNumberFormat;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issue.report.SummarizeDataImportIssues;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.graph_builder.module.configure.DaggerGraphBuilderFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.worldenvelope.service.WorldEnvelopeModel;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.transit.service.TransitModel;
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
  private final TransitModel transitModel;
  private final DataImportIssueStore issueStore;

  private boolean hasTransitData = false;

  @Inject
  public GraphBuilder(
    @Nonnull Graph baseGraph,
    @Nonnull TransitModel transitModel,
    @Nonnull DataImportIssueStore issueStore
  ) {
    this.graph = baseGraph;
    this.transitModel = transitModel;
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
    TransitModel transitModel,
    WorldEnvelopeModel worldEnvelopeModel,
    boolean loadStreetGraph,
    boolean saveStreetGraph
  ) {
    //DaggerGraphBuilderFactory appFactory = GraphBuilderFactoryDa
    boolean hasOsm = dataSources.has(OSM);
    boolean hasGtfs = dataSources.has(GTFS);
    boolean hasNetex = dataSources.has(NETEX);
    boolean hasTransitData = hasGtfs || hasNetex;

    transitModel.initTimeZone(config.transitModelTimeZone);

    var factory = DaggerGraphBuilderFactory
      .builder()
      .config(config)
      .graph(graph)
      .transitModel(transitModel)
      .worldEnvelopeModel(worldEnvelopeModel)
      .dataSources(dataSources)
      .timeZoneId(transitModel.getTimeZone())
      .build();

    var graphBuilder = factory.graphBuilder();

    graphBuilder.hasTransitData = hasTransitData;

    if (hasOsm) {
      graphBuilder.addModule(factory.openStreetMapModule());
    }

    if (hasGtfs) {
      graphBuilder.addModule(factory.gtfsModule());
    }

    if (hasNetex) {
      graphBuilder.addModule(factory.netexModule());
    }

    if (hasTransitData) {
      graphBuilder.addModule(factory.tripPatternNamer());
    }

    if (hasTransitData && transitModel.getAgencyTimeZones().size() > 1) {
      graphBuilder.addModule(factory.timeZoneAdjusterModule());
    }

    if (hasTransitData && (hasOsm || graphBuilder.graph.hasStreets)) {
      if (config.matchBusRoutesToStreets) {
        graphBuilder.addModule(factory.busRouteStreetMatcher());
      }
      graphBuilder.addModule(factory.osmBoardingLocationsModule());
    }

    // This module is outside the hasGTFS conditional block because it also links things like bike rental
    // which need to be handled even when there's no transit.
    graphBuilder.addModule(factory.streetLinkerModule());

    // Prune graph connectivity islands after transit stop linking, so that pruning can take into account
    // existence of stops in islands. If an island has a stop, it actually may be a real island and should
    // not be removed quite as easily
    if ((hasOsm && !saveStreetGraph) || loadStreetGraph) {
      graphBuilder.addModule(factory.pruneNoThruIslands());
    }

    // Load elevation data and apply it to the streets.
    // We want to do run this module after loading the OSM street network but before finding transfers.
    for (GraphBuilderModule it : factory.elevationModules()) {
      graphBuilder.addModule(it);
    }

    if (hasTransitData) {
      // Add links to flex areas after the streets has been split, so that also the split edges are connected
      if (OTPFeature.FlexRouting.isOn()) {
        graphBuilder.addModule(factory.flexLocationsToStreetEdgesMapper());
      }

      // This module will use streets or straight line distance depending on whether OSM data is found in the graph.
      graphBuilder.addModule(factory.directTransferGenerator());

      // Analyze routing between stops to generate report
      if (OTPFeature.TransferAnalyzer.isOn()) {
        graphBuilder.addModule(factory.directTransferAnalyzer());
      }
    }

    if (loadStreetGraph || hasOsm) {
      graphBuilder.addModule(factory.graphCoherencyCheckerModule());
    }

    if (config.dataImportReport) {
      graphBuilder.addModule(factory.dataImportIssuesToHTML());
    }

    if (OTPFeature.DataOverlay.isOn()) {
      graphBuilder.addModuleOptional(factory.dataOverlayFactory());
    }

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

    new SummarizeDataImportIssues(issueStore.listIssues()).summarize();

    validate();

    logGraphBuilderCompleteStatus(startTime, graph, transitModel);
  }

  private void addModule(GraphBuilderModule module) {
    graphBuilderModules.add(module);
  }

  private void addModuleOptional(GraphBuilderModule module) {
    if (module != null) {
      graphBuilderModules.add(module);
    }
  }

  private boolean hasTransitData() {
    return hasTransitData;
  }

  /**
   * Validates the build. Currently, only checks if the graph has transit data if any transit data
   * sets were included in the build. If all transit data gets filtered out due to transit period
   * configuration, for example, then this function will throw a {@link OtpAppException}.
   */
  private void validate() {
    if (hasTransitData() && !transitModel.hasTransit()) {
      throw new OtpAppException(
        "The provided transit data have no trips within the configured transit " +
        "service period. See build config 'transitServiceStart' and " +
        "'transitServiceEnd'"
      );
    }
  }

  private static void logGraphBuilderCompleteStatus(
    long startTime,
    Graph graph,
    TransitModel transitModel
  ) {
    long endTime = System.currentTimeMillis();
    String time = DurationUtils.durationToStr(Duration.ofMillis(endTime - startTime));
    var f = new OtpNumberFormat();
    var nStops = f.formatNumber(transitModel.getStopModel().stopIndexSize());
    var nPatterns = f.formatNumber(transitModel.getAllTripPatterns().size());
    var nTransfers = f.formatNumber(transitModel.getTransferService().listAll().size());
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
