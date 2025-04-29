package org.opentripplanner.ext.smoovebikerental;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalSystem;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.DataSource;
import org.opentripplanner.updater.spi.GenericJsonDataSource;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a VehicleRentalDataSource for the Smoove GIR SabiWeb used in Helsinki.
 *
 * @see DataSource
 */
public class SmooveBikeRentalDataSource
  extends GenericJsonDataSource<VehicleRentalPlace>
  implements VehicleRentalDataSource {

  private static final Logger LOG = LoggerFactory.getLogger(SmooveBikeRentalDataSource.class);

  public static final String DEFAULT_NETWORK_NAME = "smoove";

  private final boolean overloadingAllowed;

  private final String networkName;
  private final RentalVehicleType vehicleType;
  private final VehicleRentalSystem system;

  public SmooveBikeRentalDataSource(SmooveBikeRentalDataSourceParameters config) {
    this(config, new OtpHttpClientFactory());
  }

  public SmooveBikeRentalDataSource(
    SmooveBikeRentalDataSourceParameters config,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    super(config.url(), "result", config.httpHeaders(), otpHttpClientFactory.create(LOG));
    networkName = config.getNetwork(DEFAULT_NETWORK_NAME);
    vehicleType = RentalVehicleType.getDefaultType(networkName);
    overloadingAllowed = config.overloadingAllowed();
    system = new VehicleRentalSystem(
      networkName,
      "fi",
      "Helsinki/Espoo",
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      "Europe/Helsinki",
      null,
      null,
      null
    );
  }

  /**
   * <pre>
   * {
   *    "result" : [
   *       {
   *          "name" : "004 Hamn",
   *          "operative" : true,
   *          "coordinates" : "60.167913,24.952269",
   *          "style" : "",
   *          "avl_bikes" : 1,
   *          "free_slots" : 11,
   *          "total_slots" : 12,
   *       },
   *       ...
   *    ]
   * }
   * </pre>
   */
  @Override
  protected VehicleRentalStation parseElement(JsonNode node) {
    VehicleRentalStation station = new VehicleRentalStation();
    String[] nameParts = node.path("name").asText().split("\\s", 2);
    station.id = new FeedScopedId(networkName, nameParts[0]);
    station.name = new NonLocalizedString(nameParts[1]);
    String[] coordinates = node.path("coordinates").asText().split(",");
    try {
      station.latitude = Double.parseDouble(coordinates[0].trim());
      station.longitude = Double.parseDouble(coordinates[1].trim());
    } catch (NumberFormatException e) {
      // E.g. coordinates is empty
      LOG.warn("Error parsing bike rental station {}", station.id, e);
      return null;
    }
    if (!node.path("style").asText().equals("Station on")) {
      station.isRenting = false;
      station.isReturning = false;
      station.vehiclesAvailable = 0;
      station.spacesAvailable = 0;
      station.capacity = node.path("total_slots").asInt();
    } else {
      station.vehiclesAvailable = node.path("avl_bikes").asInt();
      station.spacesAvailable = node.path("free_slots").asInt();
      station.capacity = node.path("total_slots").asInt();
    }
    station.vehicleTypesAvailable = Map.of(vehicleType, station.vehiclesAvailable);
    station.vehicleSpacesAvailable = Map.of(vehicleType, station.spacesAvailable);
    station.overloadingAllowed = overloadingAllowed;
    station.system = system;
    return station;
  }
}
