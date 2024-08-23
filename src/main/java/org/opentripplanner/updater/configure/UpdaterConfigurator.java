package org.opentripplanner.updater.configure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.opentripplanner.ext.siri.SiriTimetableSnapshotSource;
import org.opentripplanner.ext.siri.updater.SiriETUpdater;
import org.opentripplanner.ext.siri.updater.SiriSXUpdater;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureETUpdater;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureSXUpdater;
import org.opentripplanner.ext.siri.updater.google.SiriETGooglePubsubUpdater;
import org.opentripplanner.ext.vehiclerentalservicedirectory.VehicleRentalServiceDirectoryFetcher;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.UpdatersParameters;
import org.opentripplanner.updater.alert.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.TimetableSnapshotFlush;
import org.opentripplanner.updater.trip.MqttGtfsRealtimeUpdater;
import org.opentripplanner.updater.trip.PollingTripUpdater;
import org.opentripplanner.updater.trip.TimetableSnapshotSource;
import org.opentripplanner.updater.vehicle_parking.AvailabilityDatasourceFactory;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingAvailabilityUpdater;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingDataSourceFactory;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdater;
import org.opentripplanner.updater.vehicle_position.PollingVehiclePositionUpdater;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdater;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSourceFactory;

/**
 * Sets up and starts all the graph updaters.
 * <p>
 * Updaters are instantiated based on the updater parameters contained in UpdaterConfig. Updaters
 * are then setup by providing the graph as a parameter. Finally, the updaters are added to the
 * GraphUpdaterManager.
 */
public class UpdaterConfigurator {

  private final Graph graph;
  private final TransitModel transitModel;
  private final UpdatersParameters updatersParameters;
  private final RealtimeVehicleRepository realtimeVehicleRepository;
  private final VehicleRentalRepository vehicleRentalRepository;
  private SiriTimetableSnapshotSource siriTimetableSnapshotSource = null;
  private TimetableSnapshotSource gtfsTimetableSnapshotSource = null;

  private UpdaterConfigurator(
    Graph graph,
    RealtimeVehicleRepository realtimeVehicleRepository,
    VehicleRentalRepository vehicleRentalRepository,
    TransitModel transitModel,
    UpdatersParameters updatersParameters
  ) {
    this.graph = graph;
    this.realtimeVehicleRepository = realtimeVehicleRepository;
    this.vehicleRentalRepository = vehicleRentalRepository;
    this.transitModel = transitModel;
    this.updatersParameters = updatersParameters;
  }

  public static void configure(
    Graph graph,
    RealtimeVehicleRepository realtimeVehicleRepository,
    VehicleRentalRepository vehicleRentalRepository,
    TransitModel transitModel,
    UpdatersParameters updatersParameters
  ) {
    new UpdaterConfigurator(
      graph,
      realtimeVehicleRepository,
      vehicleRentalRepository,
      transitModel,
      updatersParameters
    )
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

    configureTimetableSnapshotFlush(updaterManager);

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
      vehicleRentalRepository
    );
  }

  /**
   * @return a list of GraphUpdaters created from the configuration
   */
  private List<GraphUpdater> createUpdatersFromConfig() {
    OpeningHoursCalendarService openingHoursCalendarService = graph.getOpeningHoursCalendarService();

    List<GraphUpdater> updaters = new ArrayList<>();

    if (!updatersParameters.getVehicleRentalParameters().isEmpty()) {
      int maxHttpConnections = updatersParameters.getVehicleRentalParameters().size();
      var otpHttpClientFactory = new OtpHttpClientFactory(maxHttpConnections);
      for (var configItem : updatersParameters.getVehicleRentalParameters()) {
        var source = VehicleRentalDataSourceFactory.create(
          configItem.sourceParameters(),
          otpHttpClientFactory
        );
        updaters.add(
          new VehicleRentalUpdater(configItem, source, graph.getLinker(), vehicleRentalRepository)
        );
      }
    }
    for (var configItem : updatersParameters.getGtfsRealtimeAlertsUpdaterParameters()) {
      updaters.add(new GtfsRealtimeAlertsUpdater(configItem, provideGtfsTransitService()));
    }
    for (var configItem : updatersParameters.getPollingStoptimeUpdaterParameters()) {
      updaters.add(
        new PollingTripUpdater(
          configItem,
          provideGtfsTransitService(),
          provideGtfsTimetableSnapshot()
        )
      );
    }
    for (var configItem : updatersParameters.getVehiclePositionsUpdaterParameters()) {
      updaters.add(
        new PollingVehiclePositionUpdater(
          configItem,
          realtimeVehicleRepository,
          provideGtfsTransitService()
        )
      );
    }
    for (var configItem : updatersParameters.getSiriETUpdaterParameters()) {
      updaters.add(
        new SiriETUpdater(configItem, provideSiriTransitService(), provideSiriTimetableSnapshot())
      );
    }
    for (var configItem : updatersParameters.getSiriETGooglePubsubUpdaterParameters()) {
      updaters.add(
        new SiriETGooglePubsubUpdater(
          configItem,
          provideSiriTransitService(),
          provideSiriTimetableSnapshot()
        )
      );
    }
    for (var configItem : updatersParameters.getSiriSXUpdaterParameters()) {
      updaters.add(new SiriSXUpdater(configItem, provideSiriTransitService()));
    }
    for (var configItem : updatersParameters.getMqttGtfsRealtimeUpdaterParameters()) {
      updaters.add(
        new MqttGtfsRealtimeUpdater(
          configItem,
          provideGtfsTransitService(),
          provideGtfsTimetableSnapshot()
        )
      );
    }
    for (var configItem : updatersParameters.getVehicleParkingUpdaterParameters()) {
      switch (configItem.updateType()) {
        case FULL -> {
          var source = VehicleParkingDataSourceFactory.create(
            configItem,
            openingHoursCalendarService
          );
          updaters.add(
            new VehicleParkingUpdater(
              configItem,
              source,
              graph.getLinker(),
              graph.getVehicleParkingService()
            )
          );
        }
        case AVAILABILITY_ONLY -> {
          var source = AvailabilityDatasourceFactory.create(configItem);
          updaters.add(
            new VehicleParkingAvailabilityUpdater(
              configItem,
              source,
              graph.getVehicleParkingService()
            )
          );
        }
      }
    }
    for (var configItem : updatersParameters.getSiriAzureETUpdaterParameters()) {
      updaters.add(
        new SiriAzureETUpdater(
          configItem,
          provideSiriTransitService(),
          provideSiriTimetableSnapshot()
        )
      );
    }
    for (var configItem : updatersParameters.getSiriAzureSXUpdaterParameters()) {
      updaters.add(new SiriAzureSXUpdater(configItem, provideSiriTransitService()));
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

  /**
   * Provide a TransitService aware of the latest (uncommitted) SIRI real-time updates.
   * Should be injected only in SIRI updaters.
   */
  private TransitService provideSiriTransitService() {
    return new DefaultTransitService(
      transitModel,
      provideSiriTimetableSnapshot().getTimetableSnapshotBuffer()
    );
  }

  private TimetableSnapshotSource provideGtfsTimetableSnapshot() {
    if (gtfsTimetableSnapshotSource == null) {
      this.gtfsTimetableSnapshotSource =
        new TimetableSnapshotSource(updatersParameters.timetableSnapshotParameters(), transitModel);
    }
    return gtfsTimetableSnapshotSource;
  }

  /**
   * Provide a TransitService aware of the latest (uncommitted) GTFS real-time updates.
   * Should be injected only in GTFS-RT updaters.
   */
  private TransitService provideGtfsTransitService() {
    return new DefaultTransitService(
      transitModel,
      provideGtfsTimetableSnapshot().getTimetableSnapshotBuffer()
    );
  }

  /**
   * If SIRI or GTFS real-time updaters are in use, configure a periodic flush of the timetable
   * snapshot.
   */
  private void configureTimetableSnapshotFlush(GraphUpdaterManager updaterManager) {
    if (siriTimetableSnapshotSource != null || gtfsTimetableSnapshotSource != null) {
      updaterManager
        .getScheduler()
        .scheduleWithFixedDelay(
          new TimetableSnapshotFlush(siriTimetableSnapshotSource, gtfsTimetableSnapshotSource),
          0,
          updatersParameters.timetableSnapshotParameters().maxSnapshotFrequency().toSeconds(),
          TimeUnit.SECONDS
        );
    }
  }
}
