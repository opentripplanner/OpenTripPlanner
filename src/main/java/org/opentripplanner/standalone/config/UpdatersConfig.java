package org.opentripplanner.standalone.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import javax.annotation.Nullable;
import org.opentripplanner.standalone.config.sandbox.BikeRentalServiceDirectoryFetcherConfig;
import org.opentripplanner.ext.bikerentalservicedirectory.api.BikeRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.ext.siri.updater.SiriETGooglePubsubUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriETUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriSXUpdaterParameters;
import org.opentripplanner.ext.siri.updater.SiriVMUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.BikeParkUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.MqttGtfsRealtimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriETGooglePubsubUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterConfig;
import org.opentripplanner.updater.UpdatersParameters;
import org.opentripplanner.updater.alerts.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.updater.bike_park.BikeParkUpdaterParameters;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdaterParameters;
import org.opentripplanner.updater.stoptime.MqttGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.stoptime.PollingStoptimeUpdaterParameters;
import org.opentripplanner.updater.stoptime.WebsocketGtfsRealtimeUpdaterParameters;
import org.opentripplanner.updater.street_notes.WFSNotePollingGraphUpdaterParameters;
import org.opentripplanner.util.OtpAppException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * This class maps between the JSON array of updaters and the concrete class implementations of
 * each updater parameters. Some updaters use the same parameters, so a map is kept between the
 * JSON updater type strings and the appropriate updater parameter class.
 */
public class UpdatersConfig implements UpdatersParameters {

  private static final String BIKE_RENTAL = "bike-rental";
  private static final String STOP_TIME_UPDATER = "stop-time-updater";
  private static final String WEBSOCKET_GTFS_RT_UPDATER = "websocket-gtfs-rt-updater";
  private static final String MQTT_GTFS_RT_UPDATER = "mqtt-gtfs-rt-updater";
  private static final String REAL_TIME_ALERTS = "real-time-alerts";
  private static final String BIKE_PARK = "bike-park";
  private static final String WINKKI_POLLING_UPDATER = "winkki-polling-updater";
  private static final String SIRI_ET_UPDATER = "siri-et-updater";
  private static final String SIRI_ET_GOOGLE_PUBSUB_UPDATER = "siri-et-google-pubsub-updater";
  private static final String SIRI_VM_UPDATER = "siri-vm-updater";
  private static final String SIRI_SX_UPDATER = "siri-sx-updater";

  private static final Map<String, BiFunction<String, NodeAdapter, ?>> CONFIG_CREATORS = new HashMap<>();

  static {
    CONFIG_CREATORS.put(BIKE_RENTAL, BikeRentalUpdaterConfig::create);
    CONFIG_CREATORS.put(BIKE_PARK, BikeParkUpdaterConfig::create);
    CONFIG_CREATORS.put(STOP_TIME_UPDATER, PollingStoptimeUpdaterConfig::create);
    CONFIG_CREATORS.put(WEBSOCKET_GTFS_RT_UPDATER, WebsocketGtfsRealtimeUpdaterConfig::create);
    CONFIG_CREATORS.put(MQTT_GTFS_RT_UPDATER, MqttGtfsRealtimeUpdaterConfig::create);
    CONFIG_CREATORS.put(REAL_TIME_ALERTS, GtfsRealtimeAlertsUpdaterConfig::create);
    CONFIG_CREATORS.put(WINKKI_POLLING_UPDATER, WFSNotePollingGraphUpdaterConfig::create);
    CONFIG_CREATORS.put(SIRI_ET_UPDATER, SiriETUpdaterConfig::create);
    CONFIG_CREATORS.put(SIRI_ET_GOOGLE_PUBSUB_UPDATER, SiriETGooglePubsubUpdaterConfig::create);
    CONFIG_CREATORS.put(SIRI_VM_UPDATER, SiriVMUpdaterConfig::create);
    CONFIG_CREATORS.put(SIRI_SX_UPDATER, SiriSXUpdaterConfig::create);
  }

  private final Multimap<String, Object> configList = ArrayListMultimap.create();

  @Nullable
  private final BikeRentalServiceDirectoryFetcherParameters bikeRentalServiceDirectoryFetcherParameters;

  public UpdatersConfig(NodeAdapter rootAdapter) {
    this.bikeRentalServiceDirectoryFetcherParameters =
        BikeRentalServiceDirectoryFetcherConfig.create(
          rootAdapter.path("bikeRentalServiceDirectory")
        );

    List<NodeAdapter> updaters = rootAdapter.path("updaters").asList();

    for (NodeAdapter conf : updaters) {
      String type = conf.asText("type");
      BiFunction<String, NodeAdapter, ?> factory = CONFIG_CREATORS.get(type);
      if(factory == null) {
        throw new OtpAppException("The updater config type is unknown: " + type);
      }
      configList.put(type, factory.apply(type, conf));
    }
  }

  /**
   * This is the endpoint url used for the BikeRentalServiceDirectory sandbox feature.
   * @see org.opentripplanner.ext.bikerentalservicedirectory.BikeRentalServiceDirectoryFetcher
   */
  @Override
  @Nullable
  public BikeRentalServiceDirectoryFetcherParameters getBikeRentalServiceDirectoryFetcherParameters() {
   return this.bikeRentalServiceDirectoryFetcherParameters;
  }

  @Override
  public List<BikeRentalUpdaterParameters> getBikeRentalParameters() {
    return getParameters(BIKE_RENTAL);
  }

  @Override
  public List<GtfsRealtimeAlertsUpdaterParameters> getGtfsRealtimeAlertsUpdaterParameters() {
    return getParameters(REAL_TIME_ALERTS);
  }

  @Override
  public List<PollingStoptimeUpdaterParameters> getPollingStoptimeUpdaterParameters() {
    return getParameters(STOP_TIME_UPDATER);
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
  public List<SiriVMUpdaterParameters> getSiriVMUpdaterParameters() {
    return getParameters(SIRI_VM_UPDATER);
  }

  @Override
  public List<WebsocketGtfsRealtimeUpdaterParameters> getWebsocketGtfsRealtimeUpdaterParameters() {
    return getParameters(WEBSOCKET_GTFS_RT_UPDATER);
  }

  @Override
  public List<MqttGtfsRealtimeUpdaterParameters> getMqttGtfsRealtimeUpdaterParameters() {
    return getParameters(MQTT_GTFS_RT_UPDATER);
  }

  @Override
  public List<BikeParkUpdaterParameters> getBikeParkUpdaterParameters() {
    return getParameters(BIKE_PARK);
  }

  @Override
  public List<WFSNotePollingGraphUpdaterParameters> getWinkkiPollingGraphUpdaterParameters() {
    return getParameters(WINKKI_POLLING_UPDATER);
  }

  private <T> List<T> getParameters(String key) {
    return (List<T>) configList.get(key);
  }
}
