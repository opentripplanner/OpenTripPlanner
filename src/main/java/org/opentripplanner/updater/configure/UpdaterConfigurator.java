package org.opentripplanner.updater.configure;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdater;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.ext.siri.updater.SiriVMUpdater;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureETUpdater;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureSXUpdater;
import org.opentripplanner.ext.vehiclerentalservicedirectory.VehicleRentalServiceDirectoryFetcher;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.vehiclepositions.VehiclePositionRepository;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.UpdatersParameters;
import org.opentripplanner.updater.alert.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.street_note.WinkkiPollingGraphUpdater;
import org.opentripplanner.updater.trip.MqttGtfsRealtimeUpdater;
import org.opentripplanner.updater.trip.PollingTripUpdater;
import org.opentripplanner.updater.trip.TimetableSnapshotSource;
import org.opentripplanner.updater.trip.WebsocketGtfsRealtimeUpdater;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingDataSourceFactory;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdater;
import org.opentripplanner.updater.vehicle_position.PollingVehiclePositionUpdater;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdater;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets up and starts all the graph updaters.
 * <p>
 * Updaters are instantiated based on the updater parameters contained in UpdaterConfig. Updaters
 * are then setup by providing the graph as a parameter. Finally, the updaters are added to the
 * GraphUpdaterManager.
 */
public class UpdaterConfigurator {

  private static final Logger LOG = LoggerFactory.getLogger(UpdaterConfigurator.class);

  private final Graph graph;
  private final TransitModel transitModel;
  private final UpdatersParameters updatersParameters;
  private final VehiclePositionRepository vehiclePositionRepository;
  private SiriTimetableSnapshotSource siriTimetableSnapshotSource = null;
  private TimetableSnapshotSource gtfsTimetableSnapshotSource = null;

  private UpdaterConfigurator(
    Graph graph,
    VehiclePositionRepository vehiclePositionRepository,
    TransitModel transitModel,
    UpdatersParameters updatersParameters
  ) {
    this.graph = graph;
    this.vehiclePositionRepository = vehiclePositionRepository;
    this.transitModel = transitModel;
    this.updatersParameters = updatersParameters;
  }

  public static void configure(
    Graph graph,
    VehiclePositionRepository vehiclePositionService,
    TransitModel transitModel,
    UpdatersParameters updatersParameters
  ) {
    new UpdaterConfigurator(graph, vehiclePositionService, transitModel, updatersParameters)
      .configure();
  }

  private void configure() {
    List<GraphUpdater> updaters = new ArrayList<>();

    updaters.addAll(createUpdatersFromConfig());

    updaters.addAll(
      // Setup updaters using the VehicleRentalServiceDirectoryFetcher(Sandbox)
      fetchVehicleRentalServicesFromOnlineDirectory(
        updatersParameters.getVehicleRentalServiceDirectoryFetcherParameters()
      )
    );

    GraphUpdaterManager updaterManager = new GraphUpdaterManager(graph, transitModel, updaters);
    updaterManager.startUpdaters();

    // Stop the updater manager if it contains nothing
    if (updaterManager.numberOfUpdaters() == 0) {
      updaterManager.stop();
    }
    // Otherwise add it to the graph
    else {
      transitModel.setUpdaterManager(updaterManager);
    }
  }

  public static void shutdownGraph(TransitModel transitModel) {
    GraphUpdaterManager updaterManager = transitModel.getUpdaterManager();
    if (updaterManager != null) {
      LOG.info("Stopping updater manager with {} updaters.", updaterManager.numberOfUpdaters());
      updaterManager.stop();
    }
  }

  /* private methods */

  /**
   * Use the online UpdaterDirectoryService to fetch VehicleRental updaters.
   */
  private List<GraphUpdater> fetchVehicleRentalServicesFromOnlineDirectory(
    VehicleRentalServiceDirectoryFetcherParameters parameters
  ) {
    if (parameters == null) {
      return List.of();
    }
    return VehicleRentalServiceDirectoryFetcher.createUpdatersFromEndpoint(
      parameters,
      graph.getLinker(),
      graph.getVehicleRentalService()
    );
  }

  /**
   * @return a list of GraphUpdaters created from the configuration
   */
  private List<GraphUpdater> createUpdatersFromConfig() {
    OpeningHoursCalendarService openingHoursCalendarService = graph.getOpeningHoursCalendarService();

    List<GraphUpdater> updaters = new ArrayList<>();

    for (var configItem : updatersParameters.getVehicleRentalParameters()) {
      var source = VehicleRentalDataSourceFactory.create(configItem.sourceParameters());
      updaters.add(
        new VehicleRentalUpdater(
          configItem,
          source,
          graph.getLinker(),
          graph.getVehicleRentalService()
        )
      );
    }
    for (var configItem : updatersParameters.getGtfsRealtimeAlertsUpdaterParameters()) {
      updaters.add(new GtfsRealtimeAlertsUpdater(configItem, transitModel));
    }
    for (var configItem : updatersParameters.getPollingStoptimeUpdaterParameters()) {
      updaters.add(
        new PollingTripUpdater(configItem, transitModel, provideGtfsTimetableSnapshot())
      );
    }
    for (var configItem : updatersParameters.getVehiclePositionsUpdaterParameters()) {
      updaters.add(
        new PollingVehiclePositionUpdater(configItem, vehiclePositionRepository, transitModel)
      );
    }
    for (var configItem : updatersParameters.getSiriETUpdaterParameters()) {
      updaters.add(new SiriETUpdater(configItem, transitModel, provideSiriTimetableSnapshot()));
    }
    for (var configItem : updatersParameters.getSiriETGooglePubsubUpdaterParameters()) {
      updaters.add(
        new SiriETGooglePubsubUpdater(configItem, transitModel, provideSiriTimetableSnapshot())
      );
    }
    for (var configItem : updatersParameters.getSiriSXUpdaterParameters()) {
      updaters.add(new SiriSXUpdater(configItem, transitModel));
    }
    for (var configItem : updatersParameters.getSiriVMUpdaterParameters()) {
      updaters.add(new SiriVMUpdater(provideSiriTimetableSnapshot(), configItem, transitModel));
    }
    for (var configItem : updatersParameters.getWebsocketGtfsRealtimeUpdaterParameters()) {
      updaters.add(
        new WebsocketGtfsRealtimeUpdater(configItem, provideGtfsTimetableSnapshot(), transitModel)
      );
    }
    for (var configItem : updatersParameters.getMqttGtfsRealtimeUpdaterParameters()) {
      updaters.add(
        new MqttGtfsRealtimeUpdater(configItem, transitModel, provideGtfsTimetableSnapshot())
      );
    }
    for (var configItem : updatersParameters.getVehicleParkingUpdaterParameters()) {
      var source = VehicleParkingDataSourceFactory.create(configItem, openingHoursCalendarService);
      updaters.add(
        new VehicleParkingUpdater(
          configItem,
          source,
          graph.getLinker(),
          graph.getVehicleParkingService()
        )
      );
    }
    for (var configItem : updatersParameters.getWinkkiPollingGraphUpdaterParameters()) {
      updaters.add(new WinkkiPollingGraphUpdater(configItem, graph));
    }
    for (var configItem : updatersParameters.getSiriAzureETUpdaterParameters()) {
      updaters.add(
        new SiriAzureETUpdater(configItem, transitModel, provideSiriTimetableSnapshot())
      );
    }
    for (var configItem : updatersParameters.getSiriAzureSXUpdaterParameters()) {
      updaters.add(new SiriAzureSXUpdater(configItem, transitModel));
    }

    return updaters;
  }

  private SiriTimetableSnapshotSource provideSiriTimetableSnapshot() {
    if (siriTimetableSnapshotSource == null) {
      this.siriTimetableSnapshotSource =
        new SiriTimetableSnapshotSource(
          updatersParameters.timetableSnapshotParameters(),
          transitModel
        );
    }

    return siriTimetableSnapshotSource;
  }

  private TimetableSnapshotSource provideGtfsTimetableSnapshot() {
    if (gtfsTimetableSnapshotSource == null) {
      this.gtfsTimetableSnapshotSource =
        new TimetableSnapshotSource(updatersParameters.timetableSnapshotParameters(), transitModel);
    }
    return gtfsTimetableSnapshotSource;
  }
}
