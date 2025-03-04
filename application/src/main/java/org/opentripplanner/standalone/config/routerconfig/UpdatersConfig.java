package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V1_5;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.BIKE_RENTAL;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.MQTT_GTFS_RT_UPDATER;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.REAL_TIME_ALERTS;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.SIRI_AZURE_ET_UPDATER;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.SIRI_AZURE_SX_UPDATER;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.SIRI_ET_GOOGLE_PUBSUB_UPDATER;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.SIRI_ET_LITE;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.SIRI_ET_UPDATER;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.SIRI_SX_LITE;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.SIRI_SX_UPDATER;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.STOP_TIME_UPDATER;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.VEHICLE_PARKING;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.VEHICLE_POSITIONS;
import static org.opentripplanner.standalone.config.routerconfig.UpdatersConfig.Type.VEHICLE_RENTAL;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import javax.annotation.Nullable;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureETUpdaterParameters;
import org.opentripplanner.ext.siri.updater.azure.SiriAzureSXUpdaterParameters;
import org.opentripplanner.ext.vehiclerentalservicedirectory.VehicleRentalServiceDirectoryFetcher;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.updaters.GtfsRealtimeAlertsUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.MqttGtfsRealtimeUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.PollingTripUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.SiriETGooglePubsubUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.SiriETLiteUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.SiriETUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.SiriSXLiteUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.SiriSXUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehicleParkingUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehicleRentalUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.azure.SiriAzureETUpdaterConfig;
import org.opentripplanner.standalone.config.routerconfig.updaters.azure.SiriAzureSXUpdaterConfig;
import org.opentripplanner.standalone.config.sandbox.VehicleRentalServiceDirectoryFetcherConfig;
import org.opentripplanner.updater.TimetableSnapshotParameters;
import org.opentripplanner.updater.UpdatersParameters;
import org.opentripplanner.updater.alert.gtfs.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.updater.alert.siri.SiriSXUpdaterParameters;
import org.opentripplanner.updater.alert.siri.lite.SiriSXLiteUpdaterParameters;
import org.opentripplanner.updater.trip.gtfs.updater.http.PollingTripUpdaterParameters;
import org.opentripplanner.updater.trip.gtfs.updater.mqtt.MqttGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.trip.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.updater.trip.siri.updater.google.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.updater.trip.siri.updater.lite.SiriETLiteUpdaterParameters;
import org.opentripplanner.updater.vehicle_parking.VehicleParkingUpdaterParameters;
import org.opentripplanner.updater.vehicle_position.VehiclePositionsUpdaterParameters;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdaterParameters;

/**
 * This class maps between the JSON array of updaters and the concrete class implementations of each
 * updater parameters. Some updaters use the same parameters, so a map is kept between the JSON
 * updater type strings and the appropriate updater parameter class.
 */
public class UpdatersConfig implements UpdatersParameters {

  private final Multimap<Type, Object> configList = ArrayListMultimap.create();

  private final TimetableSnapshotParameters timetableUpdates;

  @Nullable
  private final VehicleRentalServiceDirectoryFetcherParameters vehicleRentalServiceDirectoryFetcherParameters;

  public UpdatersConfig(NodeAdapter rootAdapter) {
    this.vehicleRentalServiceDirectoryFetcherParameters =
      VehicleRentalServiceDirectoryFetcherConfig.create(
        "vehicleRentalServiceDirectory",
        rootAdapter
      );

    timetableUpdates = timetableUpdates(
      rootAdapter
        .of("timetableUpdates")
        .since(V2_2)
        .summary("Global configuration for timetable updaters.")
        .asObject()
    );

    rootAdapter
      .of("updaters")
      .since(V1_5)
      .summary("Configuration for the updaters that import various types of data into OTP.")
      .asObjects(it -> {
        Type type = it
          .of("type")
          .since(V1_5)
          .summary("The type of the updater.")
          .asEnum(Type.class);
        var config = type.parseConfig(it);
        configList.put(type, config);
        // We do not care what we return here
        return config;
      });
  }

  /**
   * Read "timetableUpdates" parameters. These parameters are used to configure the
   * GtfsRealTimeTripUpdateAdapter. Both the GTFS and Siri version uses the same parameters.
   */
  private TimetableSnapshotParameters timetableUpdates(NodeAdapter c) {
    var dflt = TimetableSnapshotParameters.DEFAULT;
    if (c.isEmpty()) {
      return dflt;
    }

    return new TimetableSnapshotParameters(
      c
        .of("maxSnapshotFrequency")
        .since(V2_2)
        .summary("How long a snapshot should be cached.")
        .description(
          "If a timetable snapshot is requested less than this number of milliseconds after the previous snapshot, then return the same instance. " +
          "Throttles the potentially resource-consuming task of duplicating a TripPattern → Timetable map and indexing the new Timetables. " +
          "Applies to GTFS-RT and Siri updates."
        )
        .asDuration(dflt.maxSnapshotFrequency()),
      c
        .of("purgeExpiredData")
        .since(V2_2)
        .summary(
          "Should expired real-time data be purged from the graph. Apply to GTFS-RT and Siri updates."
        )
        .asBoolean(dflt.purgeExpiredData())
    );
  }

  public TimetableSnapshotParameters timetableSnapshotParameters() {
    return timetableUpdates;
  }

  /**
   * This is the endpoint url used for the VehicleRentalServiceDirectory sandbox feature.
   *
   * @see VehicleRentalServiceDirectoryFetcher
   */
  @Override
  @Nullable
  public VehicleRentalServiceDirectoryFetcherParameters getVehicleRentalServiceDirectoryFetcherParameters() {
    return this.vehicleRentalServiceDirectoryFetcherParameters;
  }

  @Override
  public List<VehicleRentalUpdaterParameters> getVehicleRentalParameters() {
    ArrayList<VehicleRentalUpdaterParameters> result = new ArrayList<>(
      getParameters(VEHICLE_RENTAL)
    );
    result.addAll(getParameters(BIKE_RENTAL));
    return result;
  }

  @Override
  public List<GtfsRealtimeAlertsUpdaterParameters> getGtfsRealtimeAlertsUpdaterParameters() {
    return getParameters(REAL_TIME_ALERTS);
  }

  @Override
  public List<PollingTripUpdaterParameters> getPollingStoptimeUpdaterParameters() {
    return getParameters(STOP_TIME_UPDATER);
  }

  @Override
  public List<VehiclePositionsUpdaterParameters> getVehiclePositionsUpdaterParameters() {
    return getParameters(VEHICLE_POSITIONS);
  }

  @Override
  public List<SiriETUpdaterParameters> getSiriETUpdaterParameters() {
    return getParameters(SIRI_ET_UPDATER);
  }

  @Override
  public List<SiriETGooglePubsubUpdaterParameters> getSiriETGooglePubsubUpdaterParameters() {
    return getParameters(SIRI_ET_GOOGLE_PUBSUB_UPDATER);
  }

  @Override
  public List<SiriSXUpdaterParameters> getSiriSXUpdaterParameters() {
    return getParameters(SIRI_SX_UPDATER);
  }

  @Override
  public List<SiriETLiteUpdaterParameters> getSiriETLiteUpdaterParameters() {
    return getParameters(SIRI_ET_LITE);
  }

  @Override
  public List<SiriSXLiteUpdaterParameters> getSiriSXLiteUpdaterParameters() {
    return getParameters(SIRI_SX_LITE);
  }

  @Override
  public List<MqttGtfsRealtimeUpdaterParameters> getMqttGtfsRealtimeUpdaterParameters() {
    return getParameters(MQTT_GTFS_RT_UPDATER);
  }

  @Override
  public List<VehicleParkingUpdaterParameters> getVehicleParkingUpdaterParameters() {
    return getParameters(VEHICLE_PARKING);
  }

  @Override
  public List<SiriAzureETUpdaterParameters> getSiriAzureETUpdaterParameters() {
    return getParameters(SIRI_AZURE_ET_UPDATER);
  }

  @Override
  public List<SiriAzureSXUpdaterParameters> getSiriAzureSXUpdaterParameters() {
    return getParameters(SIRI_AZURE_SX_UPDATER);
  }

  private <T> List<T> getParameters(Type key) {
    return (List<T>) configList.get(key);
  }

  public enum Type {
    // TODO: deprecated, remove in next major version
    BIKE_PARK(VehicleParkingUpdaterConfig::create),
    VEHICLE_PARKING(VehicleParkingUpdaterConfig::create),
    // TODO: deprecated, remove in next major version
    BIKE_RENTAL(VehicleRentalUpdaterConfig::create),
    VEHICLE_RENTAL(VehicleRentalUpdaterConfig::create),
    STOP_TIME_UPDATER(PollingTripUpdaterConfig::create),
    MQTT_GTFS_RT_UPDATER(MqttGtfsRealtimeUpdaterConfig::create),
    REAL_TIME_ALERTS(GtfsRealtimeAlertsUpdaterConfig::create),
    VEHICLE_POSITIONS(VehiclePositionsUpdaterConfig::create),
    SIRI_ET_UPDATER(SiriETUpdaterConfig::create),
    SIRI_ET_LITE(SiriETLiteUpdaterConfig::create),
    SIRI_ET_GOOGLE_PUBSUB_UPDATER(SiriETGooglePubsubUpdaterConfig::create),
    SIRI_SX_UPDATER(SiriSXUpdaterConfig::create),
    SIRI_SX_LITE(SiriSXLiteUpdaterConfig::create),
    SIRI_AZURE_ET_UPDATER(SiriAzureETUpdaterConfig::create),
    SIRI_AZURE_SX_UPDATER(SiriAzureSXUpdaterConfig::create);

    private final BiFunction<String, NodeAdapter, ?> factory;

    Type(BiFunction<String, NodeAdapter, ?> factory) {
      this.factory = factory;
    }

    Object parseConfig(NodeAdapter nodeAdapter) {
      return factory.apply(this.name(), nodeAdapter);
    }
  }
}
