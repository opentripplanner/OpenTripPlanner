package org.opentripplanner.standalone.config.updaters.sources;

import static org.opentripplanner.updater.DataSourceType.GBFS;

import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.ext.smoovebikerental.SmooveBikeRentalDataSourceParameters;
import org.opentripplanner.standalone.config.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.util.OtpAppException;

/**
 * This class is an object representation of the data source for a single real-time updater in
 * 'router-config.json' Each data source defines an inner interface with its required attributes.
 */
public class VehicleRentalSourceFactory {

  private static final Map<String, DataSourceType> CONFIG_MAPPING = new HashMap<>();

  static {
    CONFIG_MAPPING.put("gbfs", GBFS);
    CONFIG_MAPPING.put("smoove", DataSourceType.SMOOVE);
  }

  private final DataSourceType type;
  private final NodeAdapter c;

  public VehicleRentalSourceFactory(DataSourceType type, NodeAdapter c) {
    this.type = type;
    this.c = c;
  }

  public static VehicleRentalDataSourceParameters create(String typeKey, NodeAdapter c) {
    DataSourceType type = CONFIG_MAPPING.get(typeKey);
    if (type == null) {
      throw new OtpAppException("The updater source type is unknown: " + typeKey);
    }
    return new VehicleRentalSourceFactory(type, c).create();
  }


  public VehicleRentalDataSourceParameters create() {
    switch (type) {
      case GBFS:
        return new GbfsVehicleRentalDataSourceParameters(
            url(),
            language(),
            allowKeepingRentedVehicleAtDestination(),
            headers()
        );
      case SMOOVE:
        return new SmooveBikeRentalDataSourceParameters(
            url(),
            network(),
            allowOverloading(),
            headers()
        );
      default:
        return new VehicleRentalDataSourceParameters(
            type,
            url(),
            headers()
        );
    }
  }

  private String language() {
    return c.asText("language", null);
  }

  private Map<String, String> headers() {
    return c.asMap("headers", NodeAdapter::asText);
  }

  private String url() {
    return c.asText("url");
  }

  private String network() {
    return c.asText("network", null);
  }

  private boolean allowKeepingRentedVehicleAtDestination() {
    return c.asBoolean("allowKeepingRentedBicycleAtDestination", false);
  }

  private boolean allowOverloading() {
    return c.asBoolean("allowOverloading", false);
  }
}
