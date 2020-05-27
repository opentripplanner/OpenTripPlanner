package org.opentripplanner.standalone.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterConfig;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterConfig;
import org.opentripplanner.util.OtpAppException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * This class maps between the JSON array of updaters and the concrete class implementations of
 * each updater parameters. Some updaters use the same parameters, so a map is kept between the
 * JSON updater type strings and the appropriate updater parameter class.
 */
public class UpdaterConfig {

  private static final String BIKE_RENTAL = "bike-rental";
  private static final String STOP_TIME_UPDATER = "stop-time-updater";
  private static final String WEBSOCKET_GTFS_RT_UPDATER = "websocket-gtfs-rt-updater";
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
    CONFIG_CREATORS.put(REAL_TIME_ALERTS, GtfsRealtimeAlertsUpdaterConfig::new);
    CONFIG_CREATORS.put(EXAMPLE_UPDATER, PollingGraphUpdaterConfig::new);
    CONFIG_CREATORS.put(EXAMPLE_POLLING_UPDATER, PollingGraphUpdaterConfig::new);
    CONFIG_CREATORS.put(WINKKI_POLLING_UPDATER, WFSNotePollingGraphUpdaterConfig::new);
    CONFIG_CREATORS.put(SIRI_ET_UPDATER, SiriETUpdaterConfig::new);
    CONFIG_CREATORS.put(SIRI_VM_UPDATER, SiriVMUpdaterConfig::new);
    CONFIG_CREATORS.put(SIRI_SX_UPDATER, SiriSXUpdaterConfig::new);
  }

  private final Multimap<String, Object> configList = ArrayListMultimap.create();

  public UpdaterConfig(NodeAdapter updaterConfigList) {
    for (NodeAdapter conf : updaterConfigList.asList()) {
      String type = conf.asText("type");
      Function<NodeAdapter, ?> factory = CONFIG_CREATORS.get(type);
      if(factory == null) {
        throw new OtpAppException("The updater config type is unknown: " + type);
      }
      configList.put(type, factory.apply(conf));
    }
  }

  public List<BikeRentalUpdaterConfig> getBikeRentalUpdaterConfigList() {
    return getConfig(BIKE_RENTAL, BikeRentalUpdaterConfig.class);
  }

  public List<GtfsRealtimeAlertsUpdaterConfig> getGtfsRealtimeAlertsUpdaterConfigList() {
    return getConfig(REAL_TIME_ALERTS, GtfsRealtimeAlertsUpdaterConfig.class);
  }

  public List<PollingStoptimeUpdaterConfig> getPollingStoptimeUpdaterConfigList() {
    return getConfig(WINKKI_POLLING_UPDATER, PollingStoptimeUpdaterConfig.class);
  }

  public List<SiriETUpdaterConfig> getSiriETUpdaterConfigList() {
    return getConfig(SIRI_ET_UPDATER, SiriETUpdaterConfig.class);
  }

  public List<SiriSXUpdaterConfig> getSiriSXUpdaterConfigList() {
    return getConfig(SIRI_SX_UPDATER, SiriSXUpdaterConfig.class);
  }

  public List<SiriVMUpdaterConfig> getSiriVMUpdaterConfigList() {
    return getConfig(SIRI_VM_UPDATER, SiriVMUpdaterConfig.class);
  }

  public List<WebsocketGtfsRealtimeUpdaterConfig> getWebsocketGtfsRealtimeUpdaterConfigList() {
    return getConfig(WEBSOCKET_GTFS_RT_UPDATER, WebsocketGtfsRealtimeUpdaterConfig.class);
  }

  public List<PollingGraphUpdaterConfig> getBikeParkUpdaterConfigList() {
    return getConfig(BIKE_PARK, PollingGraphUpdaterConfig.class);
  }

  public List<PollingGraphUpdaterConfig> getExampleGraphUpdaterConfigList() {
    return getConfig(EXAMPLE_UPDATER, PollingGraphUpdaterConfig.class);
  }

  public List<PollingGraphUpdaterConfig> getExamplePollingGraphUpdaterConfigList() {
    return getConfig(EXAMPLE_POLLING_UPDATER, PollingGraphUpdaterConfig.class);
  }

  public List<WFSNotePollingGraphUpdaterConfig> getWinkkiPollingGraphUpdaterConfigList() {
    return getConfig(WINKKI_POLLING_UPDATER, WFSNotePollingGraphUpdaterConfig.class);
  }

  private <T> List<T> getConfig(String key, Class<T> type) {
    return (List<T>) configList.get(key);
  }
}
