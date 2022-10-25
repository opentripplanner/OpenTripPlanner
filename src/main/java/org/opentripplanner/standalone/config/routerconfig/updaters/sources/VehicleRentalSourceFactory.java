package org.opentripplanner.standalone.config.routerconfig.updaters.sources;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.ext.smoovebikerental.SmooveBikeRentalDataSourceParameters;
import org.opentripplanner.ext.vilkkubikerental.VilkkuBikeRentalDataSourceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.DataSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;
import org.opentripplanner.util.OtpAppException;

/**
 * This class is an object representation of the data source for a single real-time updater in
 * 'router-config.json' Each data source defines an inner interface with its required attributes.
 */
public class VehicleRentalSourceFactory {

  private static final Set<DataSourceType> CONFIG_MAPPING = EnumSet.of(
    DataSourceType.GBFS,
    DataSourceType.SMOOVE,
    DataSourceType.VILKKU
  );
  private final DataSourceType type;
  private final NodeAdapter c;

  public VehicleRentalSourceFactory(DataSourceType type, NodeAdapter c) {
    this.type = type;
    this.c = c;
  }

  public static VehicleRentalDataSourceParameters create(DataSourceType type, NodeAdapter c) {
    if (!CONFIG_MAPPING.contains(type)) {
      throw new OtpAppException("The updater source type is not supported: " + type);
    }

    return new VehicleRentalSourceFactory(type, c).create();
  }

  public VehicleRentalDataSourceParameters create() {
    return switch (type) {
      case GBFS -> new GbfsVehicleRentalDataSourceParameters(
        url(),
        language(),
        allowKeepingRentedVehicleAtDestination(),
        headers(),
        network()
      );
      case SMOOVE -> new SmooveBikeRentalDataSourceParameters(
        url(),
        network(),
        allowOverloading(),
        headers()
      );
      case VILKKU -> new VilkkuBikeRentalDataSourceParameters(
        url(),
        network(),
        allowOverloading(),
        headers()
      );
      default -> new VehicleRentalDataSourceParameters(type, url(), headers());
    };
  }

  private String language() {
    return c.of("language").withDoc(NA, /*TODO DOC*/"TODO").asString(null);
  }

  private Map<String, String> headers() {
    return c.of("headers").withDoc(NA, /*TODO DOC*/"TODO").asStringMap();
  }

  private String url() {
    return c.of("url").withDoc(NA, /*TODO DOC*/"TODO").asString();
  }

  private String network() {
    return c.of("network").withDoc(NA, /*TODO DOC*/"TODO").asString(null);
  }

  private boolean allowKeepingRentedVehicleAtDestination() {
    return c
      .of("allowKeepingRentedBicycleAtDestination")
      .withDoc(NA, /*TODO DOC*/"TODO")
      .asBoolean(false);
  }

  private boolean allowOverloading() {
    return c.of("allowOverloading").withDoc(NA, /*TODO DOC*/"TODO").asBoolean(false);
  }
}
