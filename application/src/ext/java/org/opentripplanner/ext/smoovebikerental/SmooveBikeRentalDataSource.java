package org.opentripplanner.ext.smoovebikerental;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.opentripplanner.framework.i18n.I18NString;
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
    system = VehicleRentalSystem.of()
      .withSystemId(networkName)
      .withName(I18NString.of("Helsinki/Espoo"))
      .build();
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
    String[] nameParts = node.path("name").asText().split("\\s", 2);
    FeedScopedId stationId = new FeedScopedId(networkName, nameParts[0]);
    String[] coordinates = node.path("coordinates").asText().split(",");

    double latitude;
    double longitude;

    try {
      latitude = Double.parseDouble(coordinates[0].trim());
      longitude = Double.parseDouble(coordinates[1].trim());
    } catch (NumberFormatException e) {
      // E.g. coordinates is empty
      LOG.warn("Error parsing bike rental station {}", stationId, e);
      return null;
    }

    var builder = VehicleRentalStation.of()
      .withId(stationId)
      .withName(new NonLocalizedString(nameParts[1]))
      .withLatitude(latitude)
      .withLongitude(longitude)
      .withOverloadingAllowed(overloadingAllowed)
      .withSystem(system)
      .withCapacity(node.path("total_slots").asInt());

    if (!node.path("style").asText().equals("Station on")) {
      builder
        .withIsRenting(false)
        .withIsReturning(false)
        .withVehiclesAvailable(0)
        .withSpacesAvailable(0)
        .withVehicleTypesAvailable(Map.of(vehicleType, 0))
        .withVehicleSpacesAvailable(Map.of(vehicleType, 0));
    } else {
      int vehiclesAvailable = node.path("avl_bikes").asInt();
      int spacesAvailable = node.path("free_slots").asInt();
      builder
        .withVehiclesAvailable(vehiclesAvailable)
        .withSpacesAvailable(spacesAvailable)
        .withVehicleTypesAvailable(Map.of(vehicleType, vehiclesAvailable))
        .withVehicleSpacesAvailable(Map.of(vehicleType, spacesAvailable));
    }

    return builder.build();
  }
}
