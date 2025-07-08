package org.opentripplanner.updater.configure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureUpdater;
import org.opentripplanner.ext.vehiclerentalservicedirectory.VehicleRentalServiceDirectoryFetcher;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.DefaultRealTimeUpdateContext;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.UpdatersParameters;
import org.opentripplanner.updater.alert.gtfs.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.TimetableSnapshotFlush;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.gtfs.updater.http.PollingTripUpdater;
import org.opentripplanner.updater.trip.gtfs.updater.mqtt.MqttGtfsRealtimeUpdater;
import org.opentripplanner.updater.trip.siri.SiriRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.siri.updater.google.SiriETGooglePubsubUpdater;
import org.opentripplanner.updater.vehicle_parking.AvailabilityDataSourceFactory;
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
  private final VertexLinker linker;
  private final TimetableRepository timetableRepository;
  private final UpdatersParameters updatersParameters;
  private final RealtimeVehicleRepository realtimeVehicleRepository;
  private final VehicleRentalRepository vehicleRentalRepository;
  private final VehicleParkingRepository parkingRepository;
  private final TimetableSnapshotManager snapshotManager;

  private UpdaterConfigurator(
    Graph graph,
    VertexLinker linker,
    RealtimeVehicleRepository realtimeVehicleRepository,
    VehicleRentalRepository vehicleRentalRepository,
    VehicleParkingRepository parkingRepository,
    TimetableRepository timetableRepository,
    TimetableSnapshotManager snapshotManager,
    UpdatersParameters updatersParameters
  ) {
    this.graph = graph;
    this.linker = linker;
    this.realtimeVehicleRepository = realtimeVehicleRepository;
    this.vehicleRentalRepository = vehicleRentalRepository;
    this.timetableRepository = timetableRepository;
    this.updatersParameters = updatersParameters;
    this.parkingRepository = parkingRepository;
    this.snapshotManager = snapshotManager;
  }

  public static void configure(
    Graph graph,
    VertexLinker linker,
    RealtimeVehicleRepository realtimeVehicleRepository,
    VehicleRentalRepository vehicleRentalRepository,
    VehicleParkingRepository parkingRepository,
    TimetableRepository timetableRepository,
    TimetableSnapshotManager snapshotManager,
    UpdatersParameters updatersParameters
  ) {
    new UpdaterConfigurator(
      graph,
      linker,
      realtimeVehicleRepository,
      vehicleRentalRepository,
      parkingRepository,
      timetableRepository,
      snapshotManager,
      updatersParameters
    ).configure();
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

    TimetableSnapshot timetableSnapshotBuffer = snapshotManager.getTimetableSnapshotBuffer();
    GraphUpdaterManager updaterManager = new GraphUpdaterManager(
      new DefaultRealTimeUpdateContext(graph, timetableRepository, timetableSnapshotBuffer),
      updaters
    );

    configureTimetableSnapshotFlush(updaterManager, snapshotManager);

    updaterManager.startUpdaters();

    // Stop the updater manager if it contains nothing
    if (updaterManager.numberOfUpdaters() == 0) {
      updaterManager.stop();
    }
    // Otherwise add it to the graph
    else {
      timetableRepository.setUpdaterManager(updaterManager);
    }
  }

  public static void shutdownGraph(TimetableRepository timetableRepository) {
    GraphUpdaterManager updaterManager = timetableRepository.getUpdaterManager();
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
      linker,
      vehicleRentalRepository
    );
  }

  /**
   * @return a list of GraphUpdaters created from the configuration
   */
  private List<GraphUpdater> createUpdatersFromConfig() {
    OpeningHoursCalendarService openingHoursCalendarService =
      graph.getOpeningHoursCalendarService();

    List<GraphUpdater> updaters = new ArrayList<>();

    if (!updatersParameters.getVehicleRentalParameters().isEmpty()) {
      int maxHttpConnections = updatersParameters.getVehicleRentalParameters().size();
      var otpHttpClientFactory = new OtpHttpClientFactory(maxHttpConnections);
      for (var configItem : updatersParameters.getVehicleRentalParameters()) {
        var source = VehicleRentalDataSourceFactory.create(
          configItem.sourceParameters(),
          otpHttpClientFactory
        );
        updaters.add(new VehicleRentalUpdater(configItem, source, linker, vehicleRentalRepository));
      }
    }
    for (var configItem : updatersParameters.getGtfsRealtimeAlertsUpdaterParameters()) {
      updaters.add(new GtfsRealtimeAlertsUpdater(configItem, timetableRepository));
    }
    for (var configItem : updatersParameters.getPollingStoptimeUpdaterParameters()) {
      updaters.add(new PollingTripUpdater(configItem, provideGtfsAdapter()));
    }
    for (var configItem : updatersParameters.getVehiclePositionsUpdaterParameters()) {
      updaters.add(new PollingVehiclePositionUpdater(configItem, realtimeVehicleRepository));
    }
    for (var configItem : updatersParameters.getSiriETUpdaterParameters()) {
      updaters.add(SiriUpdaterModule.createSiriETUpdater(configItem, provideSiriAdapter()));
    }
    for (var configItem : updatersParameters.getSiriETLiteUpdaterParameters()) {
      updaters.add(SiriUpdaterModule.createSiriETUpdater(configItem, provideSiriAdapter()));
    }
    for (var configItem : updatersParameters.getSiriETGooglePubsubUpdaterParameters()) {
      updaters.add(new SiriETGooglePubsubUpdater(configItem, provideSiriAdapter()));
    }
    for (var configItem : updatersParameters.getSiriSXUpdaterParameters()) {
      updaters.add(SiriUpdaterModule.createSiriSXUpdater(configItem, timetableRepository));
    }
    for (var configItem : updatersParameters.getSiriSXLiteUpdaterParameters()) {
      updaters.add(SiriUpdaterModule.createSiriSXUpdater(configItem, timetableRepository));
    }
    for (var configItem : updatersParameters.getMqttGtfsRealtimeUpdaterParameters()) {
      updaters.add(new MqttGtfsRealtimeUpdater(configItem, provideGtfsAdapter()));
    }
    for (var configItem : updatersParameters.getVehicleParkingUpdaterParameters()) {
      switch (configItem.updateType()) {
        case FULL -> {
          var source = VehicleParkingDataSourceFactory.create(
            configItem,
            openingHoursCalendarService
          );
          updaters.add(new VehicleParkingUpdater(configItem, source, linker, parkingRepository));
        }
        case AVAILABILITY_ONLY -> {
          var source = AvailabilityDataSourceFactory.create(configItem);
          updaters.add(
            new VehicleParkingAvailabilityUpdater(configItem, source, parkingRepository)
          );
        }
      }
    }
    for (var configItem : updatersParameters.getSiriAzureETUpdaterParameters()) {
      updaters.add(SiriAzureUpdater.createETUpdater(configItem, provideSiriAdapter()));
    }
    for (var configItem : updatersParameters.getSiriAzureSXUpdaterParameters()) {
      updaters.add(SiriAzureUpdater.createSXUpdater(configItem, timetableRepository));
    }

    return updaters;
  }

  private SiriRealTimeTripUpdateAdapter provideSiriAdapter() {
    return new SiriRealTimeTripUpdateAdapter(timetableRepository, snapshotManager);
  }

  private GtfsRealTimeTripUpdateAdapter provideGtfsAdapter() {
    return new GtfsRealTimeTripUpdateAdapter(timetableRepository, snapshotManager, () ->
      LocalDate.now(timetableRepository.getTimeZone())
    );
  }

  /**
   * If SIRI or GTFS real-time updaters are in use, configure a periodic flush of the timetable
   * snapshot.
   */
  private void configureTimetableSnapshotFlush(
    GraphUpdaterManager updaterManager,
    TimetableSnapshotManager snapshotManager
  ) {
    updaterManager
      .getScheduler()
      .scheduleWithFixedDelay(
        new TimetableSnapshotFlush(snapshotManager),
        0,
        updatersParameters.timetableSnapshotParameters().maxSnapshotFrequency().toSeconds(),
        TimeUnit.SECONDS
      );
  }
}
