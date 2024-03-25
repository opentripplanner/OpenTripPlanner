package org.opentripplanner.ext.vehicleparking.noi;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.GenericJsonDataSource;

/**
 * Vehicle parking updater class for NOI's open data hub format APIs.
 */
public class NoiUpdater extends GenericJsonDataSource<VehicleParking> {

  private static final String JSON_PARSE_PATH = "data/stations";

  private final String feedId;

  public NoiUpdater(NoiUpdaterParameters parameters) {
    super(parameters.url().toString(), JSON_PARSE_PATH, parameters.httpHeaders());
    this.feedId = parameters.feedId();
  }

  @Override
  protected VehicleParking parseElement(JsonNode jsonNode) {
    var type = jsonNode.path("type").textValue();
    VehicleParkingSpaces capacity, availability;
    if (type.equals("station")) {
      capacity = extractSpaces(jsonNode, "capacity");
      availability = extractSpaces(jsonNode, "free");
    } else if (type.equals("sensor")) {
      capacity = VehicleParkingSpaces.builder().carSpaces(1).build();
      var isFree = jsonNode.path("free").asBoolean();
      availability = VehicleParkingSpaces.builder().carSpaces(isFree ? 1 : 0).build();
    } else {
      throw new IllegalArgumentException("Unknown type '%s'".formatted(type));
    }

    var vehicleParkId = new FeedScopedId(feedId, jsonNode.path("station_id").asText());
    var name = new NonLocalizedString(jsonNode.path("name").asText());
    double lat = jsonNode.path("lat").asDouble();
    double lon = jsonNode.path("lon").asDouble();
    var coordinate = new WgsCoordinate(lat, lon);
    VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
      builder
        .entranceId(new FeedScopedId(feedId, vehicleParkId.getId() + "/entrance"))
        .coordinate(coordinate)
        .walkAccessible(true)
        .carAccessible(true);

    return VehicleParking
      .builder()
      .id(vehicleParkId)
      .name(name)
      .state(VehicleParkingState.OPERATIONAL)
      .coordinate(coordinate)
      .capacity(capacity)
      .availability(availability)
      .carPlaces(true)
      .entrance(entrance)
      .build();
  }

  private static VehicleParkingSpaces extractSpaces(JsonNode jsonNode, String free) {
    return VehicleParkingSpaces.builder().carSpaces(jsonNode.get(free).asInt()).build();
  }
}
