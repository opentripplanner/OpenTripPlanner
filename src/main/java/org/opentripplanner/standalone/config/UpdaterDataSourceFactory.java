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

  private static final Map<String, Entry> CONFIG_CREATORS = new HashMap<>();

  static {
    add("b-cycle", DataSourceType.B_CYCLE, UpdaterSourceConfig::new);
    add("bicimad", DataSourceType.BICIMAD, UpdaterSourceConfig::new);
    add("bixi", DataSourceType.BIXI, UpdaterSourceConfig::new);
    add("city-bikes", DataSourceType.CITY_BIKES, UpdaterSourceConfig::new);
    add("citi-bike-nyc", DataSourceType.CITI_BIKE_NYC, UpdaterSourceConfig::new);
    add("gbfs", DataSourceType.GBFS, GbfsSourceConfig::new);
    add("jcdecaux", DataSourceType.JCDECAUX, UpdaterSourceConfig::new);
    add("keolis-rennes", DataSourceType.KEOLIS_RENNES, UpdaterSourceConfig::new);
    add("kml", DataSourceType.KML, GenericKmlBikeRentalSourceConfig::new);
    add("kml-bike-park", DataSourceType.KML_BIKE_PARK, KmlBikeParkSourceConfig::new);
    add("next-bike", DataSourceType.NEXT_BIKE, UpdaterSourceConfig::new);
    add("ov-fiets", DataSourceType.OV_FIETS, UpdaterSourceConfig::new);
    add("sf-bay-area", DataSourceType.SF_BAY_AREA, UpdaterSourceConfig::new);
    add("share-bike", DataSourceType.SHARE_BIKE, UpdaterSourceConfig::new);
    add("smoove", DataSourceType.SMOOVE, UpdaterSourceConfig::new);
    add("uip-bike", DataSourceType.UIP_BIKE, UpdaterSourceConfig::new);
    add("vcub", DataSourceType.VCUV, UpdaterSourceConfig::new);
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
