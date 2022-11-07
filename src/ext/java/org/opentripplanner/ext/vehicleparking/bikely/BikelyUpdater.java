package org.opentripplanner.ext.vehicleparking.bikely;

import static org.opentripplanner.routing.vehicle_parking.VehicleParkingState.OPERATIONAL;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.GenericJsonDataSource;

/**
 * Vehicle parking updater class for the Norwegian bike box provider Bikely: https://www.safebikely.com/
 */
class BikelyUpdater extends GenericJsonDataSource<VehicleParking> {

  private static final String JSON_PARSE_PATH = "result";

  private final String feedId;

  public BikelyUpdater(BikelyUpdaterParameters parameters) {
    super(parameters.url(), JSON_PARSE_PATH, parameters.httpHeaders());
    this.feedId = parameters.feedId();
  }

  @Override
  protected VehicleParking parseElement(JsonNode jsonNode) {
    var vehicleParkId = new FeedScopedId(feedId, jsonNode.get("id").asText());

    var addressNode = jsonNode.get("address");
    var workingHoursNode = jsonNode.get("workingHours");

    var lat = addressNode.get("latitude").asDouble();
    var lng = addressNode.get("longitude").asDouble();
    var coord = new WgsCoordinate(lat, lng);

    var freeSpots = jsonNode.get("availableParkingSpots").asInt();
    var isUnderMaintenance = workingHoursNode.get("isUnderMaintenance").asBoolean();

    VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
      builder
        .entranceId(new FeedScopedId(feedId, vehicleParkId.getId() + "/entrance"))
        .name(new NonLocalizedString(jsonNode.path("name").asText()))
        .coordinate(coord)
        .walkAccessible(true)
        .carAccessible(false);

    return VehicleParking
      .builder()
      .id(vehicleParkId)
      .name(new NonLocalizedString(jsonNode.path("name").asText()))
      .bicyclePlaces(true)
      .availability(VehicleParkingSpaces.builder().bicycleSpaces(freeSpots).build())
      .state(toStatus(isUnderMaintenance))
      .coordinate(coord)
      .entrance(entrance)
      .build();
  }

  private VehicleParkingState toStatus(boolean isUnderMaintenance) {
    if (isUnderMaintenance) return VehicleParkingState.TEMPORARILY_CLOSED; else return OPERATIONAL;
  }
}
