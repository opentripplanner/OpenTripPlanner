package org.opentripplanner.standalone.config;

import org.opentripplanner.standalone.config.updaters.sources.*;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.UpdaterDataSourceParameters;
import org.opentripplanner.util.OtpAppException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * This class is an object representation of the data source for a single real-time updater in
 * 'router-config.json' Each data source defines an inner interface with its required attributes.
 */
public class UpdaterDataSourceFactory {

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

  private static final Map<String, Entry> CONFIG_CREATORS = new HashMap<>();

  static {
    add(B_CYCLE, DataSourceType.B_CYCLE, UpdaterSourceConfig::new);
    add(BICIMAD, DataSourceType.BICIMAD, UpdaterSourceConfig::new);
    add(BIXI, DataSourceType.BIXI, UpdaterSourceConfig::new);
    add(CITY_BIKES, DataSourceType.CITY_BIKES, UpdaterSourceConfig::new);
    add(CITI_BIKE_NYC, DataSourceType.CITI_BIKE_NYC, UpdaterSourceConfig::new);
    add(GBFS, DataSourceType.GBFS, GbfsSourceConfig::new);
    add(JCDECAUX, DataSourceType.JCDECAUX, UpdaterSourceConfig::new);
    add(KEOLIS_RENNES, DataSourceType.KEOLIS_RENNES, UpdaterSourceConfig::new);
    add(KML, DataSourceType.KML, GenericKmlBikeRentalSourceConfig::new);
    add(KML_BIKE_PARK, DataSourceType.KML_BIKE_PARK, KmlBikeParkSourceConfig::new);
    add(NEXT_BIKE, DataSourceType.NEXT_BIKE, UpdaterSourceConfig::new);
    add(OV_FIETS, DataSourceType.OV_FIETS, UpdaterSourceConfig::new);
    add(SF_BAY_AREA, DataSourceType.SF_BAY_AREA, UpdaterSourceConfig::new);
    add(SHARE_BIKE, DataSourceType.SHARE_BIKE, UpdaterSourceConfig::new);
    add(SIRI_ET, DataSourceType.SIRI_ET, SiriETHttpTripUpdaterSourceConfig::new);
    add(SIRI_ET_PUBSUB, DataSourceType.SIRI_ET_PUBSUB, UpdaterSourceConfig::new);
    add(SIRI_VM, DataSourceType.SIRI_VM, SiriVMHttpTripUpdaterSourceConfig::new);
    add(SIRI_SX, DataSourceType.SIRI_SX, UpdaterSourceConfig::new);
    add(SMOOVE, DataSourceType.SMOOVE, UpdaterSourceConfig::new);
    add(UIP_BIKE, DataSourceType.UIP_BIKE, UpdaterSourceConfig::new);
    add(VCUV, DataSourceType.VCUV, UpdaterSourceConfig::new);
  }

  public static UpdaterDataSourceParameters createDataSourceParameters(String type, NodeAdapter sourceConfig) {
    Entry entry = CONFIG_CREATORS.get(type);
    if (entry == null) {
      throw new OtpAppException("The updater source type is unknown: " + type);
    }
    return entry.create(sourceConfig);
  }

  private static void add(
      String ref, DataSourceType type, BiFunction<DataSourceType, NodeAdapter, UpdaterSourceConfig> factory
  ) {
    CONFIG_CREATORS.put(ref, new Entry(type, factory));
  }

  private static class Entry {
    private final DataSourceType type;
    private final BiFunction<DataSourceType, NodeAdapter, UpdaterSourceConfig> factory;

    public Entry(
        DataSourceType type,
        BiFunction<DataSourceType, NodeAdapter, UpdaterSourceConfig> factory
    ) {
      this.type = type;
      this.factory = factory;
    }

    UpdaterDataSourceParameters create(NodeAdapter sourceConfig) {
      return factory.apply(type, sourceConfig);
    }
  }
}
