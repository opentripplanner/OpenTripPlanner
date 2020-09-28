package org.opentripplanner.standalone.config;

import org.opentripplanner.standalone.config.updaters.sources.GbfsSourceConfig;
import org.opentripplanner.standalone.config.updaters.sources.GenericKmlBikeRentalSourceConfig;
import org.opentripplanner.standalone.config.updaters.sources.KmlBikeParkSourceConfig;
import org.opentripplanner.standalone.config.updaters.sources.SiriETHttpTripUpdaterSourceConfig;
import org.opentripplanner.standalone.config.updaters.sources.SiriVMHttpTripUpdaterSourceConfig;
import org.opentripplanner.standalone.config.updaters.sources.UpdaterSourceConfig;
import org.opentripplanner.updater.UpdaterDataSourceConfig;
import org.opentripplanner.updater.UpdaterDataSourceParameters;
import org.opentripplanner.util.OtpAppException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * This class is an object representation of the data source for a single real-time updater in
 * 'router-config.json' Each data source defines an inner interface with its required attributes.
 */
public class DefaultUpdaterDataSourceConfig implements UpdaterDataSourceConfig {

  public static final String B_CYCLE = "b-cycle";
  public static final String BICIMAD = "bicimad";
  public static final String BIXI = "bixi";
  public static final String CITY_BIKES = "city-bikes";
  public static final String CITI_BIKE_NYC = "citi-bike-nyc";
  public static final String GBFS = "gbfs";
  public static final String JCDECAUX = "jcdecaux";
  public static final String KEOLIS_RENNES = "keolis-rennes";
  public static final String KML = "kml";
  public static final String KML_BIKE_PARK = "kml-bike-park";
  public static final String NEXT_BIKE = "next-bike";
  public static final String OV_FIETS = "ov-fiets";
  public static final String SF_BAY_AREA = "sf-bay-area";
  public static final String SHARE_BIKE = "share-bike";
  public static final String SIRI_ET = "siri-et";
  public static final String SIRI_ET_PUBSUB = "siri-et-pubsub";
  public static final String SIRI_VM = "siri-vm";
  public static final String SIRI_SX = "siri-sx";
  public static final String SMOOVE = "smoove";
  public static final String UIP_BIKE = "uip-bike";
  public static final String VCUV = "vcub";

  private static final Map<String, Function<NodeAdapter, UpdaterSourceConfig>> CONFIG_CREATORS
      = new HashMap<>();

  static {
    CONFIG_CREATORS.put(B_CYCLE, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(BICIMAD, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(BIXI, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(CITY_BIKES, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(CITI_BIKE_NYC, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(GBFS, GbfsSourceConfig::new);
    CONFIG_CREATORS.put(JCDECAUX, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(KEOLIS_RENNES, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(KML, GenericKmlBikeRentalSourceConfig::new);
    CONFIG_CREATORS.put(KML_BIKE_PARK, KmlBikeParkSourceConfig::new);
    CONFIG_CREATORS.put(NEXT_BIKE, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(OV_FIETS, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(SF_BAY_AREA, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(SHARE_BIKE, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(SIRI_ET, SiriETHttpTripUpdaterSourceConfig::new);
    CONFIG_CREATORS.put(SIRI_ET_PUBSUB, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(SIRI_VM, SiriVMHttpTripUpdaterSourceConfig::new);
    CONFIG_CREATORS.put(SIRI_SX, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(SMOOVE, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(UIP_BIKE, UpdaterSourceConfig::new);
    CONFIG_CREATORS.put(VCUV, UpdaterSourceConfig::new);
  }

  private final String type;

  private final UpdaterSourceConfig updaterSourceParameters;

  public DefaultUpdaterDataSourceConfig(NodeAdapter sourceConfig) {
    String type = sourceConfig.asText("sourceType");
    Function<NodeAdapter, UpdaterSourceConfig> factory = CONFIG_CREATORS.get(type);
    if (factory == null) {
      throw new OtpAppException("The updater config type is unknown: " + type);
    }
    this.type = type;
    this.updaterSourceParameters = factory.apply(sourceConfig);
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public UpdaterDataSourceParameters getUpdaterSourceParameters() {
    return updaterSourceParameters;
  }
}
