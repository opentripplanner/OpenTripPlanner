package org.opentripplanner.standalone.config.updaters.sources;

import static org.opentripplanner.updater.DataSourceType.GBFS;
import static org.opentripplanner.updater.DataSourceType.KML;

import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.bike_rental.datasources.params.BikeRentalDataSourceParameters;
import org.opentripplanner.updater.bike_rental.datasources.params.GbfsBikeRentalDataSourceParameters;
import org.opentripplanner.updater.bike_rental.datasources.params.GenericKmlBikeRentalDataSourceParameters;
import org.opentripplanner.util.OtpAppException;

/**
 * This class is an object representation of the data source for a single real-time updater in
 * 'router-config.json' Each data source defines an inner interface with its required attributes.
 */
public class BikeRentalSourceFactory {

  private static final Map<String, DataSourceType> CONFIG_MAPPING = new HashMap<>();

  static {
    CONFIG_MAPPING.put("b-cycle", DataSourceType.B_CYCLE);
    CONFIG_MAPPING.put("bicimad", DataSourceType.BICIMAD);
    CONFIG_MAPPING.put("bixi", DataSourceType.BIXI);
    CONFIG_MAPPING.put("city-bikes", DataSourceType.CITY_BIKES);
    CONFIG_MAPPING.put("citi-bike-nyc", DataSourceType.CITI_BIKE_NYC);
    CONFIG_MAPPING.put("gbfs", GBFS);
    CONFIG_MAPPING.put("jcdecaux", DataSourceType.JCDECAUX);
    CONFIG_MAPPING.put("keolis-rennes", DataSourceType.KEOLIS_RENNES);
    CONFIG_MAPPING.put("kml", KML);
    CONFIG_MAPPING.put("next-bike", DataSourceType.NEXT_BIKE);
    CONFIG_MAPPING.put("ov-fiets", DataSourceType.OV_FIETS);
    CONFIG_MAPPING.put("sf-bay-area", DataSourceType.SF_BAY_AREA);
    CONFIG_MAPPING.put("share-bike", DataSourceType.SHARE_BIKE);
    CONFIG_MAPPING.put("smoove", DataSourceType.SMOOVE);
    CONFIG_MAPPING.put("uip-bike", DataSourceType.UIP_BIKE);
    CONFIG_MAPPING.put("vcub", DataSourceType.VCUV);
  }

  private final DataSourceType type;
  private final NodeAdapter c;

  public BikeRentalSourceFactory(DataSourceType type, NodeAdapter c) {
    this.type = type;
    this.c = c;
  }

  public static BikeRentalDataSourceParameters create(String typeKey, NodeAdapter c) {
    DataSourceType type = CONFIG_MAPPING.get(typeKey);
    if (type == null) {
      throw new OtpAppException("The updater source type is unknown: " + typeKey);
    }
    return new BikeRentalSourceFactory(type, c).create();
  }


  public BikeRentalDataSourceParameters create() {
    switch (type) {
      case GBFS: return new GbfsBikeRentalDataSourceParameters(url(), network(), routeAsCar(), allowKeepingBicycleRentalsAtDestination());
      case KML:  return new GenericKmlBikeRentalDataSourceParameters(url(), namePrefix());
      default:   return new BikeRentalDataSourceParameters(type, url(), network(), apiKey());
    }
  }

  private String url() {
    return c.asText("url");
  }

  private String network() {
    return c.asText("network", null);
  }

  private String apiKey() {
    return c.asText("apiKey", null);
  }

  private String namePrefix() {
    return c.asText("namePrefix", null);
  }

  private boolean routeAsCar() {
    return c.asBoolean("routeAsCar", false);
  }

  private boolean allowKeepingBicycleRentalsAtDestination() {
    return c.asBoolean("allowKeepingRentedBicycleAtDestination", false);
  }
}
