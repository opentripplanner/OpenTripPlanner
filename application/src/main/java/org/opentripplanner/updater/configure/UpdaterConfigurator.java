package org.opentripplanner.updater.configure;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.carpooling.updater.SiriETCarpoolingUpdater;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureUpdater;
import org.opentripplanner.ext.siri.updater.mqtt.SiriETMqttUpdater;
import org.opentripplanner.ext.vehiclerentalservicedirectory.VehicleRentalServiceDirectoryFetcher;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.transaction.UpdateManager;
import org.opentripplanner.framework.transaction.api.RepositoryHandle;
import org.opentripplanner.service.realtimevehicles.RealtimeVehicleRepository;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.transit.repository.TimetableRepository;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.transit.service.TransitRepository;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.GraphWriterService;
import org.opentripplanner.updater.UpdatersParameters;
import org.opentripplanner.updater.alert.gtfs.GtfsRealtimeAlertsUpdater;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.spi.WriteDomain;
import org.opentripplanner.updater.spi.WriteToGraphCallbacks;
import org.opentripplanner.updater.trip.gtfs.GtfsRealTimeTripUpdateAdapter;
import org.opentripplanner.updater.trip.gtfs.updater.http.PollingTripUpdater;
import org.opentripplanner.updater.trip.gtfs.updater.mqtt.MqttGtfsRealtimeUpdater;
import org.opentripplanner.updater.trip.siri.SiriFuzzyTripMatcherCache;
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
  private final DeduplicatorService deduplicator;
  private final VertexLinker linker;
  private final TransitRepository transitRepository;
  private final UpdatersParameters updatersParameters;
  private final RealtimeVehicleRepository realtimeVehicleRepository;
  private final VehicleRentalRepository vehicleRentalRepository;

  /** {@code null} when {@link OTPFeature#CarPooling} is off. */
  @Nullable
  private final CarpoolingRepository carpoolingRepository;

  /** {@code null} when {@link OTPFeature#CarPooling} is off. */
  @Nullable
  private final CarpoolTripVertexResolver carpoolTripVertexResolver;

  private final VehicleParkingRepository parkingRepository;
  private final UpdateManager transitUpdateManager;
  private final UpdateManager streetUpdateManager;
  private final RepositoryHandle<
    TimetableRepositorySnapshot,
    TimetableRepository
  > timetableRepositoryHandle;

  @Nullable
  private SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache;

  private UpdaterConfigurator(
    Graph graph,
    DeduplicatorService deduplicator,
    VertexLinker linker,
    RealtimeVehicleRepository realtimeVehicleRepository,
    VehicleRentalRepository vehicleRentalRepository,
    VehicleParkingRepository parkingRepository,
    TransitRepository transitRepository,
    @Nullable CarpoolingRepository carpoolingRepository,
    @Nullable CarpoolTripVertexResolver carpoolTripVertexResolver,
    UpdateManager transitUpdateManager,
    UpdateManager streetUpdateManager,
    RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableRepositoryHandle,
    UpdatersParameters updatersParameters
  ) {
    this.graph = graph;
    this.deduplicator = deduplicator;
    this.linker = linker;
    this.realtimeVehicleRepository = realtimeVehicleRepository;
    this.vehicleRentalRepository = vehicleRentalRepository;
    this.transitRepository = transitRepository;
    this.updatersParameters = updatersParameters;
    this.parkingRepository = parkingRepository;
    this.transitUpdateManager = transitUpdateManager;
    this.streetUpdateManager = streetUpdateManager;
    this.timetableRepositoryHandle = timetableRepositoryHandle;
    this.carpoolingRepository = carpoolingRepository;
    this.carpoolTripVertexResolver = carpoolTripVertexResolver;
  }

  public static void configure(
    Graph graph,
    DeduplicatorService deduplicator,
    VertexLinker linker,
    RealtimeVehicleRepository realtimeVehicleRepository,
    VehicleRentalRepository vehicleRentalRepository,
    VehicleParkingRepository parkingRepository,
    TransitRepository transitRepository,
    @Nullable CarpoolingRepository carpoolingRepository,
    @Nullable CarpoolTripVertexResolver carpoolTripVertexResolver,
    UpdateManager transitUpdateManager,
    UpdateManager streetUpdateManager,
    RepositoryHandle<TimetableRepositorySnapshot, TimetableRepository> timetableRepositoryHandle,
    UpdatersParameters updatersParameters
  ) {
    new UpdaterConfigurator(
      graph,
      deduplicator,
      linker,
      realtimeVehicleRepository,
      vehicleRentalRepository,
      parkingRepository,
      transitRepository,
      carpoolingRepository,
      carpoolTripVertexResolver,
      transitUpdateManager,
      streetUpdateManager,
      timetableRepositoryHandle,
      updatersParameters
    ).configure();
  }

  private void configure() {
    List<GraphUpdater<?>> updaters = new ArrayList<>();

    updaters.addAll(createUpdatersFromConfig());

    updaters.addAll(
      // Setup updaters using the VehicleRentalServiceDirectoryFetcher(Sandbox)
      fetchVehicleRentalServicesFromOnlineDirectory(
        updatersParameters.getVehicleRentalServiceDirectoryFetcherParameters()
      )
    );

    var transitWriterService = GraphWriterService.forTransitDomain(
      transitUpdateManager,
      timetableRepositoryHandle,
      transitRepository
    );
    var streetWriterService = GraphWriterService.forStreetDomain(streetUpdateManager, graph);
    var updaterManager = new GraphUpdaterManager(
      new WriteToGraphCallbacks()
        .with(WriteDomain.TRANSIT, transitWriterService)
        .with(WriteDomain.STREET, streetWriterService),
      () -> {
        try {
          transitWriterService.stop();
        } finally {
          streetWriterService.stop();
        }
      },
      updaters
    );

    updaterManager.startUpdaters();

    // Stop the updater manager if it contains nothing
    if (updaterManager.numberOfUpdaters() == 0) {
      updaterManager.stop();
    }
    // Otherwise add it to the graph
    else {
      transitRepository.initUpdaterManager(updaterManager);
    }
  }

  public static void shutdownGraph(TransitRepository transitRepository) {
    GraphUpdaterManager updaterManager = transitRepository.getUpdaterManager();
    if (updaterManager != null) {
      updaterManager.stop();
    }
  }

  /* private methods */

  /**
   * Use the online UpdaterDirectoryService to fetch VehicleRental updaters.
   */
  private List<GraphUpdater<?>> fetchVehicleRentalServicesFromOnlineDirectory(
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
  private List<GraphUpdater<?>> createUpdatersFromConfig() {
    OpeningHoursCalendarService openingHoursCalendarService =
      graph.getOpeningHoursCalendarService();

    List<GraphUpdater<?>> updaters = new ArrayList<>();

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
      updaters.add(new GtfsRealtimeAlertsUpdater(configItem, transitRepository));
    }
    for (var configItem : updatersParameters.getPollingStoptimeUpdaterParameters()) {
      updaters.add(new PollingTripUpdater(configItem, provideGtfsAdapter()));
    }
    for (var configItem : updatersParameters.getVehiclePositionsUpdaterParameters()) {
      updaters.add(new PollingVehiclePositionUpdater(configItem, realtimeVehicleRepository));
    }
    for (var configItem : updatersParameters.getSiriETUpdaterParameters()) {
      updaters.add(
        SiriUpdaterModule.createSiriETUpdater(
          configItem,
          provideSiriAdapter(configItem.fuzzyTripMatching())
        )
      );
    }
    if (OTPFeature.CarPooling.isOn()) {
      for (var configItem : updatersParameters.getSiriETCarpoolingUpdaterParameters()) {
        updaters.add(
          new SiriETCarpoolingUpdater(configItem, carpoolingRepository, carpoolTripVertexResolver)
        );
      }
    }
    for (var configItem : updatersParameters.getSiriETLiteUpdaterParameters()) {
      updaters.add(
        SiriUpdaterModule.createSiriETUpdater(
          configItem,
          provideSiriAdapter(configItem.fuzzyTripMatching())
        )
      );
    }
    for (var configItem : updatersParameters.getSiriETGooglePubsubUpdaterParameters()) {
      updaters.add(
        new SiriETGooglePubsubUpdater(
          configItem,
          provideSiriAdapter(configItem.fuzzyTripMatching())
        )
      );
    }
    for (var configItem : updatersParameters.getSiriSXUpdaterParameters()) {
      updaters.add(
        SiriUpdaterModule.createSiriSXUpdater(
          configItem,
          transitRepository,
          siriFuzzyTripMatcherCache()
        )
      );
    }
    for (var configItem : updatersParameters.getSiriSXLiteUpdaterParameters()) {
      updaters.add(
        SiriUpdaterModule.createSiriSXUpdater(
          configItem,
          transitRepository,
          siriFuzzyTripMatcherCache()
        )
      );
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
      updaters.add(
        SiriAzureUpdater.createETUpdater(
          configItem,
          provideSiriAdapter(configItem.isFuzzyTripMatching())
        )
      );
    }
    for (var configItem : updatersParameters.getSiriAzureSXUpdaterParameters()) {
      updaters.add(
        SiriAzureUpdater.createSXUpdater(configItem, transitRepository, siriFuzzyTripMatcherCache())
      );
    }
    for (var configItem : updatersParameters.getMqttSiriETUpdaterParameters()) {
      updaters.add(
        new SiriETMqttUpdater(configItem, provideSiriAdapter(configItem.fuzzyTripMatching()))
      );
    }

    return updaters;
  }

  private SiriRealTimeTripUpdateAdapter provideSiriAdapter(boolean fuzzyTripMatching) {
    var cache = fuzzyTripMatching ? siriFuzzyTripMatcherCache() : null;
    return new SiriRealTimeTripUpdateAdapter(transitRepository, deduplicator, cache);
  }

  private SiriFuzzyTripMatcherCache siriFuzzyTripMatcherCache() {
    if (siriFuzzyTripMatcherCache == null) {
      siriFuzzyTripMatcherCache = new SiriFuzzyTripMatcherCache(transitRepository);
    }
    return siriFuzzyTripMatcherCache;
  }

  private GtfsRealTimeTripUpdateAdapter provideGtfsAdapter() {
    return new GtfsRealTimeTripUpdateAdapter(transitRepository, deduplicator, () ->
      LocalDate.now(transitRepository.getTimeZone())
    );
  }
}
