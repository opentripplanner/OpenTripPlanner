package org.opentripplanner.standalone.config.routerconfig.updaters.sources;

import static org.opentripplanner.standalone.config.framework.json.EnumMapper.docEnumValueList;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V1_5;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_2;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_3;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.util.Set;
import org.opentripplanner.ext.smoovebikerental.SmooveBikeRentalDataSourceParameters;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;
import org.opentripplanner.standalone.config.routerconfig.updaters.HttpHeadersConfig;
import org.opentripplanner.updater.spi.HttpHeaders;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalSourceType;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
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
        network(),
        geofencingZones(),
        overloadingAllowed(),
        rentalPickupTypes()
      );
      case SMOOVE -> new SmooveBikeRentalDataSourceParameters(
        url(),
        network(),
        overloadingAllowed(),
        headers(),
        rentalPickupTypes()
      );
    };
  }

  private String language() {
    return c.of("language").since(V2_1).summary("TODO").asString(null);
  }

  private HttpHeaders headers() {
    return HttpHeadersConfig.headers(c, V1_5);
  }

  private String url() {
    return c.of("url").since(V1_5).summary("The URL to download the data from.").asString();
  }

  private String network() {
    return c
      .of("network")
      .since(V1_5)
      .summary("The name of the network to override the one derived from the source data.")
      .description(
        "GBFS feeds must include a system_id which will be used as the default `network`. These " +
        "ids are sometimes not helpful so setting this property will override it."
      )
      .asString(null);
  }

  private boolean allowKeepingRentedVehicleAtDestination() {
    return c
      .of("allowKeepingRentedVehicleAtDestination")
      .since(V2_1)
      .summary("If a vehicle should be allowed to be kept at the end of a station-based rental.")
      .description(
        """
        In some cases it may be useful to not drop off the rented vehicle before arriving at the destination.
        This is useful if vehicles may only be rented for round trips, or the destination is an intermediate place.

        For this to be possible three things need to be configured:

         - In the updater configuration `allowKeepingRentedVehicleAtDestination` should be set to `true`.
         - `allowKeepingRentedVehicleAtDestination` should also be set for each request, either using routing defaults, or per-request.
         - If keeping the vehicle at the destination should be discouraged, then `keepingRentedVehicleAtDestinationCost` (default: 0) may also be set in the routing defaults.
        """
      )
      .asBoolean(false);
  }

  private boolean overloadingAllowed() {
    return c
      .of("overloadingAllowed")
      .since(V2_2)
      .summary("Allow leaving vehicles at a station even though there are no free slots.")
      .asBoolean(false);
  }

  private boolean geofencingZones() {
    return c
      .of("geofencingZones")
      .since(V2_3)
      .summary("Compute rental restrictions based on GBFS 2.2 geofencing zones.")
      .description(
        """
        This feature is somewhat experimental and therefore turned off by default for the following reasons:

        - It delays start up of OTP. How long is dependent on the complexity of the zones. For example in Oslo it takes 6 seconds to compute while Portland takes 25 seconds.
        - It's easy for a malformed or unintended geofencing zone to make routing impossible. If you encounter such a case, please file a bug report.
        """
      )
      .asBoolean(false);
  }

  private Set<RentalPickupType> rentalPickupTypes() {
    return c
      .of("rentalPickupTypes")
      .since(V2_7)
      .summary(RentalPickupType.STATION.typeDescription())
      .description(docEnumValueList(RentalPickupType.values()))
      .asEnumSet(RentalPickupType.class, RentalPickupType.ALL);
  }
}
