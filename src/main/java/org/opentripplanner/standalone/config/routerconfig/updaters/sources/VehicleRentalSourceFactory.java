package org.opentripplanner.standalone.config.routerconfig.updaters.sources;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.Map;
import org.opentripplanner.ext.smoovebikerental.SmooveBikeRentalDataSourceParameters;
import org.opentripplanner.ext.vilkkubikerental.VilkkuBikeRentalDataSourceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.VehicleRentalDataSourceParameters;

/**
 * This class is an object representation of the data source for a single real-time updater in
 * 'router-config.json' Each data source defines an inner interface with its required attributes.
 */
public class VehicleRentalSourceFactory {

  private final VehicleRentalSourceType type;
  private final NodeAdapter c;

  public VehicleRentalSourceFactory(VehicleRentalSourceType type, NodeAdapter c) {
    this.type = type;
    this.c = c;
  }

  public static VehicleRentalDataSourceParameters create(
    VehicleRentalSourceType type,
    NodeAdapter c
  ) {
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
    };
  }

  private String language() {
    return c.of("language").since(NA).summary("TODO").asString(null);
  }

  private Map<String, String> headers() {
    return c
      .of("headers")
      .since(NA)
      .summary("HTTP headers to add to the request. Any header key, value can be inserted.")
      .asStringMap();
  }

  private String url() {
    return c.of("url").since(NA).summary("The URL to download the data from.").asString();
  }

  private String network() {
    return c
      .of("network")
      .since(NA)
      .summary("The name of the network to override the one derived from the source data.")
      .description(
        "GBFS feeds must include a system_id which will be used as the default `network`. These " +
        "ids are sometimes not helpful so setting this property will override it."
      )
      .asString(null);
  }

  private boolean allowKeepingRentedVehicleAtDestination() {
    return c
      .of("allowKeepingRentedBicycleAtDestination")
      .since(NA)
      .summary("If a vehicle should be allowed to be kept at the end of a station-based rental.")
      .description(
        "This behaviour is useful in towns that have only a single rental station. Without it you " +
        "would need see any results as you would have to always bring it back to the station."
      )
      .asBoolean(false);
  }

  private boolean allowOverloading() {
    return c
      .of("allowOverloading")
      .since(NA)
      .summary("Allow leaving vehicles at a station even though there are no free slots.")
      .asBoolean(false);
  }
}
