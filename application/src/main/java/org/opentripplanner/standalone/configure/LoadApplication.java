package org.opentripplanner.standalone.configure;

import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.graph_builder.GraphBuilderDataSources;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueSummary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.SerializedGraphObject;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.standalone.config.CommandLineParameters;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.street.model.StreetLimitationParameters;
import org.opentripplanner.transit.service.TimetableRepository;

/**
 * This class is responsible for loading configuration and setting up the OTP data store.
 * This is used to load the graph, and finally this class can create the
 * {@link ConstructApplication} for the next phase.
 * <p>
 * By splitting these two responsibilities into two separate phases we are sure all
 * components (graph and transit model) created in the load phase will be available for
 * creating the application using Dagger dependency injection.
 */
public class LoadApplication {

  private final CommandLineParameters cli;
  private final LoadApplicationFactory factory;

  private boolean dataStoreLoaded = false;

  /**
   * Create a new OTP configuration instance for a given directory.
   */
  public LoadApplication(CommandLineParameters commandLineParameters) {
    this.cli = commandLineParameters;
    this.factory = DaggerLoadApplicationFactory.builder().commandLineParameters(cli).build();
  }

  public void validateConfigAndDataSources() {
    // Load Graph Builder Data Sources to validate it.
    factory.graphBuilderDataSources();
    this.dataStoreLoaded = true;
  }

  public DataSource getInputGraphDataStore() {
    return cli.doLoadGraph()
      ? factory.datastore().getGraph()
      : factory.datastore().getStreetGraph();
  }

  /** Construct application from serialized graph */
  public ConstructApplication appConstruction(SerializedGraphObject obj) {
    return createAppConstruction(
      obj.graph,
      obj.osmInfoGraphBuildRepository,
      obj.timetableRepository,
      obj.worldEnvelopeRepository,
      obj.parkingRepository,
      obj.issueSummary,
      obj.emissionsDataModel,
      obj.stopConsolidationRepository,
      obj.streetLimitationParameters
    );
  }

  /** Construct application with an empty model. */
  public ConstructApplication appConstruction() {
    return createAppConstruction(
      factory.emptyGraph(),
      factory.emptyOsmInfoGraphBuildRepository(),
      factory.emptyTimetableRepository(),
      factory.emptyWorldEnvelopeRepository(),
      factory.emptyVehicleParkingRepository(),
      DataImportIssueSummary.empty(),
      factory.emptyEmissionsDataModel(),
      factory.emptyStopConsolidationRepository(),
      factory.emptyStreetLimitationParameters()
    );
  }

  public GraphBuilderDataSources graphBuilderDataSources() {
    if (!dataStoreLoaded) {
      throw new IllegalStateException("Validate graphBuilderDataSources before using it");
    }
    return factory.graphBuilderDataSources();
  }

  public ConfigModel config() {
    return factory.configModel();
  }

  private ConstructApplication createAppConstruction(
    Graph graph,
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    TimetableRepository timetableRepository,
    WorldEnvelopeRepository worldEnvelopeRepository,
    VehicleParkingRepository parkingRepository,
    DataImportIssueSummary issueSummary,
    @Nullable EmissionsDataModel emissionsDataModel,
    @Nullable StopConsolidationRepository stopConsolidationRepository,
    StreetLimitationParameters streetLimitationParameters
  ) {
    return new ConstructApplication(
      cli,
      graph,
      osmInfoGraphBuildRepository,
      timetableRepository,
      worldEnvelopeRepository,
      config(),
      graphBuilderDataSources(),
      issueSummary,
      emissionsDataModel,
      parkingRepository,
      stopConsolidationRepository,
      streetLimitationParameters
    );
  }
}
