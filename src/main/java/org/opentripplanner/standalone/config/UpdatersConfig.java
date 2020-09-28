package org.opentripplanner.standalone.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.MqttGtfsRealtimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterConfig;
import org.opentripplanner.updater.UpdatersParameters;
import org.opentripplanner.util.OtpAppException;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

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
  private static final String EXAMPLE_UPDATER = "example-updater";
  private static final String EXAMPLE_POLLING_UPDATER = "example-polling-updater";
  private static final String WINKKI_POLLING_UPDATER = "winkki-polling-updater";
  private static final String SIRI_ET_UPDATER = "siri-et-updater";
  private static final String SIRI_VM_UPDATER = "siri-vm-updater";
  private static final String SIRI_SX_UPDATER = "siri-sx-updater";

  private static final Map<String, Function<NodeAdapter, ?>> CONFIG_CREATORS = new HashMap<>();

  static {
    CONFIG_CREATORS.put(BIKE_RENTAL, BikeRentalUpdaterConfig::new);
    CONFIG_CREATORS.put(BIKE_PARK, PollingGraphUpdaterConfig::new);
    CONFIG_CREATORS.put(STOP_TIME_UPDATER, PollingStoptimeUpdaterConfig::new);
    CONFIG_CREATORS.put(WEBSOCKET_GTFS_RT_UPDATER, WebsocketGtfsRealtimeUpdaterConfig::new);
    CONFIG_CREATORS.put(MQTT_GTFS_RT_UPDATER, MqttGtfsRealtimeUpdaterConfig::new);
    CONFIG_CREATORS.put(REAL_TIME_ALERTS, GtfsRealtimeAlertsUpdaterConfig::new);
    CONFIG_CREATORS.put(EXAMPLE_UPDATER, PollingGraphUpdaterConfig::new);
    CONFIG_CREATORS.put(EXAMPLE_POLLING_UPDATER, PollingGraphUpdaterConfig::new);
    CONFIG_CREATORS.put(WINKKI_POLLING_UPDATER, WFSNotePollingGraphUpdaterConfig::new);
    CONFIG_CREATORS.put(SIRI_ET_UPDATER, SiriETUpdaterConfig::new);
    CONFIG_CREATORS.put(SIRI_VM_UPDATER, SiriVMUpdaterConfig::new);
    CONFIG_CREATORS.put(SIRI_SX_UPDATER, SiriSXUpdaterConfig::new);
  }

  private final Multimap<String, Object> configList = ArrayListMultimap.create();

  private final URI bikeRentalServiceDirectoryUrl;

  public UpdatersConfig(NodeAdapter rootAdapter) {
    this.bikeRentalServiceDirectoryUrl = rootAdapter.asUri("bikeRentalServiceDirectoryUrl", null);

    List<NodeAdapter> updaters = rootAdapter.path("updaters").asList();

    for (NodeAdapter conf : updaters) {
      String type = conf.asText("type");
      Function<NodeAdapter, ?> factory = CONFIG_CREATORS.get(type);
      if(factory == null) {
        throw new OtpAppException("The updater config type is unknown: " + type);
      }
      configList.put(type, factory.apply(conf));
    }
  }

  /**
   * This is the endpoint url used for the BikeRentalServiceDirectory sandbox feature.
   * @see org.opentripplanner.ext.bikerentalservicedirectory.BikeRentalServiceDirectoryFetcher
   */
  @Override
  public URI bikeRentalServiceDirectoryUrl() {
   return this.bikeRentalServiceDirectoryUrl;
  }

  @Override
  public List<BikeRentalUpdaterConfig> getBikeRentalParameters() {
    return getParameters(BIKE_RENTAL, BikeRentalUpdaterConfig.class);
  }

  @Override
  public List<GtfsRealtimeAlertsUpdaterConfig> getGtfsRealtimeAlertsUpdaterParameters() {
    return getParameters(REAL_TIME_ALERTS, GtfsRealtimeAlertsUpdaterConfig.class);
  }

  @Override
  public List<PollingStoptimeUpdaterConfig> getPollingStoptimeUpdaterParameters() {
    return getParameters(WINKKI_POLLING_UPDATER, PollingStoptimeUpdaterConfig.class);
  }

  @Override
  public List<SiriETUpdaterConfig> getSiriETUpdaterParameters() {
    return getParameters(SIRI_ET_UPDATER, SiriETUpdaterConfig.class);
  }

  @Override
  public List<SiriSXUpdaterConfig> getSiriSXUpdaterParameters() {
    return getParameters(SIRI_SX_UPDATER, SiriSXUpdaterConfig.class);
  }

  @Override
  public List<SiriVMUpdaterConfig> getSiriVMUpdaterParameters() {
    return getParameters(SIRI_VM_UPDATER, SiriVMUpdaterConfig.class);
  }

  @Override
  public List<WebsocketGtfsRealtimeUpdaterConfig> getWebsocketGtfsRealtimeUpdaterParameters() {
    return getParameters(WEBSOCKET_GTFS_RT_UPDATER, WebsocketGtfsRealtimeUpdaterConfig.class);
  }

  @Override
  public List<MqttGtfsRealtimeUpdaterConfig> getMqttGtfsRealtimeUpdaterParameters() {
    return getParameters(MQTT_GTFS_RT_UPDATER, MqttGtfsRealtimeUpdaterConfig.class);
  }

  @Override
  public List<PollingGraphUpdaterConfig> getBikeParkUpdaterParameters() {
    return getParameters(BIKE_PARK, PollingGraphUpdaterConfig.class);
  }

  @Override
  public List<PollingGraphUpdaterConfig> getExampleGraphUpdaterParameters() {
    return getParameters(EXAMPLE_UPDATER, PollingGraphUpdaterConfig.class);
  }

  @Override
  public List<PollingGraphUpdaterConfig> getExamplePollingGraphUpdaterParameters() {
    return getParameters(EXAMPLE_POLLING_UPDATER, PollingGraphUpdaterConfig.class);
  }

  @Override
  public List<WFSNotePollingGraphUpdaterConfig> getWinkkiPollingGraphUpdaterParameters() {
    return getParameters(WINKKI_POLLING_UPDATER, WFSNotePollingGraphUpdaterConfig.class);
  }

  private <T> List<T> getParameters(String key, Class<T> type) {
    return (List<T>) configList.get(key);
  }
}
