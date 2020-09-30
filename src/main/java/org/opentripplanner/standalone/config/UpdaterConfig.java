package org.opentripplanner.standalone.config;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.ext.siri.updater.SiriEstimatedTimetableGooglePubsubUpdater;
import org.opentripplanner.standalone.config.updaters.BikeRentalUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.GooglePubsubSiriETUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.GtfsRealtimeAlertsUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.MqttGtfsRealtimeUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.PollingGraphUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.PollingStoptimeUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriETUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriSXUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.SiriVMUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.WFSNotePollingGraphUpdaterParameters;
import org.opentripplanner.standalone.config.updaters.WebsocketGtfsRealtimeUpdaterParameters;
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
public class UpdaterConfig implements org.opentripplanner.updater.UpdaterParameters {

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
  private static final String GOOGLE_PUBSUB_SIRI_ET_UPDATER = "google-pubsub-siri-et-updater";
  private static final String SIRI_VM_UPDATER = "siri-vm-updater";
  private static final String SIRI_SX_UPDATER = "siri-sx-updater";

  private static final Map<String, Function<NodeAdapter, ?>> CONFIG_CREATORS = new HashMap<>();

  static {
    CONFIG_CREATORS.put(BIKE_RENTAL, BikeRentalUpdaterParameters::new);
    CONFIG_CREATORS.put(BIKE_PARK, PollingGraphUpdaterParameters::new);
    CONFIG_CREATORS.put(STOP_TIME_UPDATER, PollingStoptimeUpdaterParameters::new);
    CONFIG_CREATORS.put(WEBSOCKET_GTFS_RT_UPDATER, WebsocketGtfsRealtimeUpdaterParameters::new);
    CONFIG_CREATORS.put(MQTT_GTFS_RT_UPDATER, MqttGtfsRealtimeUpdaterParameters::new);
    CONFIG_CREATORS.put(REAL_TIME_ALERTS, GtfsRealtimeAlertsUpdaterParameters::new);
    CONFIG_CREATORS.put(EXAMPLE_UPDATER, PollingGraphUpdaterParameters::new);
    CONFIG_CREATORS.put(EXAMPLE_POLLING_UPDATER, PollingGraphUpdaterParameters::new);
    CONFIG_CREATORS.put(WINKKI_POLLING_UPDATER, WFSNotePollingGraphUpdaterParameters::new);
    CONFIG_CREATORS.put(SIRI_ET_UPDATER, SiriETUpdaterParameters::new);
    CONFIG_CREATORS.put(GOOGLE_PUBSUB_SIRI_ET_UPDATER, GooglePubsubSiriETUpdaterParameters::new);
    CONFIG_CREATORS.put(SIRI_VM_UPDATER, SiriVMUpdaterParameters::new);
    CONFIG_CREATORS.put(SIRI_SX_UPDATER, SiriSXUpdaterParameters::new);
  }

  private final Multimap<String, Object> configList = ArrayListMultimap.create();

  private final URI bikeRentalServiceDirectoryUrl;

  public UpdaterConfig(NodeAdapter rootAdapter) {
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
  public List<BikeRentalUpdaterParameters> getBikeRentalParameters() {
    return getParameters(BIKE_RENTAL, BikeRentalUpdaterParameters.class);
  }

  @Override
  public List<GtfsRealtimeAlertsUpdaterParameters> getGtfsRealtimeAlertsUpdaterParameters() {
    return getParameters(REAL_TIME_ALERTS, GtfsRealtimeAlertsUpdaterParameters.class);
  }

  @Override
  public List<PollingStoptimeUpdaterParameters> getPollingStoptimeUpdaterParameters() {
    return getParameters(WINKKI_POLLING_UPDATER, PollingStoptimeUpdaterParameters.class);
  }

  @Override
  public List<SiriETUpdaterParameters> getSiriETUpdaterParameters() {
    return getParameters(SIRI_ET_UPDATER, SiriETUpdaterParameters.class);
  }

  @Override
  public List<SiriEstimatedTimetableGooglePubsubUpdater.Parameters> getSiriETGooglePubsubUpdaterParameters() {
    return getParameters(GOOGLE_PUBSUB_SIRI_ET_UPDATER, SiriEstimatedTimetableGooglePubsubUpdater.Parameters.class);
  }

  @Override
  public List<SiriSXUpdaterParameters> getSiriSXUpdaterParameters() {
    return getParameters(SIRI_SX_UPDATER, SiriSXUpdaterParameters.class);
  }

  @Override
  public List<SiriVMUpdaterParameters> getSiriVMUpdaterParameters() {
    return getParameters(SIRI_VM_UPDATER, SiriVMUpdaterParameters.class);
  }

  @Override
  public List<WebsocketGtfsRealtimeUpdaterParameters> getWebsocketGtfsRealtimeUpdaterParameters() {
    return getParameters(WEBSOCKET_GTFS_RT_UPDATER, WebsocketGtfsRealtimeUpdaterParameters.class);
  }

  @Override
  public List<MqttGtfsRealtimeUpdaterParameters> getMqttGtfsRealtimeUpdaterParameters() {
    return getParameters(MQTT_GTFS_RT_UPDATER, MqttGtfsRealtimeUpdaterParameters.class);
  }

  @Override
  public List<PollingGraphUpdaterParameters> getBikeParkUpdaterParameters() {
    return getParameters(BIKE_PARK, PollingGraphUpdaterParameters.class);
  }

  @Override
  public List<PollingGraphUpdaterParameters> getExampleGraphUpdaterParameters() {
    return getParameters(EXAMPLE_UPDATER, PollingGraphUpdaterParameters.class);
  }

  @Override
  public List<PollingGraphUpdaterParameters> getExamplePollingGraphUpdaterParameters() {
    return getParameters(EXAMPLE_POLLING_UPDATER, PollingGraphUpdaterParameters.class);
  }

  @Override
  public List<WFSNotePollingGraphUpdaterParameters> getWinkkiPollingGraphUpdaterParameters() {
    return getParameters(WINKKI_POLLING_UPDATER, WFSNotePollingGraphUpdaterParameters.class);
  }

  private <T> List<T> getParameters(String key, Class<T> type) {
    return (List<T>) configList.get(key);
  }
}
