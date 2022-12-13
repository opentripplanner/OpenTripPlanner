package org.opentripplanner.ext.vilkkubikerental;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStation;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.GenericXmlDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VilkkuBikeRentalDataSource extends GenericXmlDataSource<VehicleRentalPlace> {

  private static final Logger LOG = LoggerFactory.getLogger(VilkkuBikeRentalDataSource.class);
  public static final String DEFAULT_FEED_NAME = "vilkku";
  private static final String stationXpath = "//station";
  private static final List<String> REQUIRED_STATION_PROPERTIES = List.of(
    "name",
    "externallyLockedBikes",
    "latitude",
    "longitude"
  );

  private final String network;

  /**
   * Initialize VilkkuBikeRentalDataSource.
   *
   * @param config   Vilkku data source configuration parameters.
   */
  public VilkkuBikeRentalDataSource(VilkkuBikeRentalDataSourceParameters config) {
    super(config.url(), stationXpath);
    network = config.network();
  }

  @Override
  protected VehicleRentalPlace parseElement(Map<String, String> attributes) {
    if (!checkProperties(attributes)) {
      return null;
    }

    VehicleRentalStation station = new VehicleRentalStation();
    station.id = id(attributes.get("name"));

    try {
      station.longitude = getCoordinate(attributes.get("longitude"));
      station.latitude = getCoordinate(attributes.get("latitude"));
    } catch (NumberFormatException e) {
      // E.g. coordinates is empty
      LOG.warn("Error parsing bike rental station location {}", station.id, e);
      return null;
    }

    station.name = new NonLocalizedString(attributes.get("name"));
    station.vehiclesAvailable = Integer.parseInt(attributes.get("externallyLockedBikes"));
    station.vehiclesDisabled = 0;
    station.spacesAvailable = Integer.MAX_VALUE;
    station.vehicleSpacesAvailable =
      Map.of(RentalVehicleType.getDefaultType(station.getNetwork()), station.spacesAvailable);
    station.spacesDisabled = 0;
    station.capacity = 0;

    // all stations in feed are considered operational
    station.isInstalled = true;
    station.isRenting = true;
    station.isReturning = true;

    // use parse time
    station.lastReported = Instant.now();
    return station;
  }

  private Boolean checkProperty(Map<String, String> attributes, String property) {
    if (!attributes.containsKey("name")) {
      LOG.warn(
        "Missing required property {} in Vilkku XML, cannot create bike rental station.",
        property
      );
      return false;
    }
    return true;
  }

  private boolean checkProperties(Map<String, String> attributes) {
    return REQUIRED_STATION_PROPERTIES.stream().allMatch(prop -> checkProperty(attributes, prop));
  }

  private double getCoordinate(String coordinate) {
    // for some reason the API returns coordinates with ',' as decimal separator
    if (coordinate.contains(",")) {
      return Double.parseDouble(coordinate.replace(",", "."));
    }
    return Double.parseDouble(coordinate);
  }

  private FeedScopedId id(String id) {
    return new FeedScopedId(getNetworkId(), id);
  }

  private String getNetworkId() {
    return network != null ? network : DEFAULT_FEED_NAME;
  }
}
