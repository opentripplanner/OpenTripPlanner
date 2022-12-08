package org.opentripplanner.ext.vehicleparking.bikely;

import static org.opentripplanner.routing.vehicle_parking.VehicleParkingState.OPERATIONAL;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Currency;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingSpaces;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingState;
import org.opentripplanner.transit.model.basic.LocalizedMoney;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.GenericJsonDataSource;

/**
 * Vehicle parking updater class for the Norwegian bike box provider Bikely:
 * https://www.safebikely.com/
 */
public class BikelyUpdater extends GenericJsonDataSource<VehicleParking> {

  private static final String JSON_PARSE_PATH = "result";
  private static final Currency NOK = Currency.getInstance("NOK");
  private final String feedId;

  public BikelyUpdater(BikelyUpdaterParameters parameters) {
    super(parameters.url(), JSON_PARSE_PATH, parameters.httpHeaders());
    this.feedId = parameters.feedId();
  }

  @Override
  protected VehicleParking parseElement(JsonNode jsonNode) {
    var vehicleParkId = new FeedScopedId(feedId, jsonNode.get("id").asText());

    var address = jsonNode.get("address");
    var workingHours = jsonNode.get("workingHours");

    var lat = address.get("latitude").asDouble();
    var lng = address.get("longitude").asDouble();
    var coord = new WgsCoordinate(lat, lng);

    var name = new NonLocalizedString(jsonNode.path("name").asText());

    var totalSpots = jsonNode.get("totalParkingSpots").asInt();
    var freeSpots = jsonNode.get("availableParkingSpots").asInt();
    var isUnderMaintenance = workingHours.get("isUnderMaintenance").asBoolean();

    LocalizedString note = toNote(jsonNode.get("price"));

    VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
      builder
        .entranceId(new FeedScopedId(feedId, vehicleParkId.getId() + "/entrance"))
        .name(name)
        .coordinate(coord)
        .walkAccessible(true)
        .carAccessible(false);

    return VehicleParking
      .builder()
      .id(vehicleParkId)
      .name(name)
      .bicyclePlaces(true)
      .capacity(VehicleParkingSpaces.builder().bicycleSpaces(totalSpots).build())
      .availability(VehicleParkingSpaces.builder().bicycleSpaces(freeSpots).build())
      .state(toState(isUnderMaintenance))
      .coordinate(coord)
      .entrance(entrance)
      .note(note)
      .build();
  }

  private static LocalizedString toNote(JsonNode price) {
    var startPriceAmount = price.get("startPriceAmount").asDouble();
    var mainPriceAmount = price.get("mainPriceAmount").asDouble();

    var startPriceDurationHours = price.get("startPriceDurationHours").asInt();
    var mainPriceDurationHours = price.get("mainPriceDurationHours").asInt();

    if (startPriceAmount == 0 && mainPriceAmount == 0) {
      return new LocalizedString("price.free");
    } else {
      return new LocalizedString(
        "price.startMain",
        NonLocalizedString.ofNumber(startPriceDurationHours),
        new LocalizedMoney(new Money(NOK, (int) (startPriceAmount * 100))),
        new LocalizedMoney(new Money(NOK, (int) (mainPriceAmount * 100))),
        NonLocalizedString.ofNumber(mainPriceDurationHours)
      );
    }
  }

  private static VehicleParkingState toState(boolean isUnderMaintenance) {
    if (isUnderMaintenance) return VehicleParkingState.TEMPORARILY_CLOSED; else return OPERATIONAL;
  }
}
