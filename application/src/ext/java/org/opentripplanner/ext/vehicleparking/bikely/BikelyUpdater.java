package org.opentripplanner.ext.vehicleparking.bikely;

import static org.opentripplanner.service.vehicleparking.model.VehicleParkingState.OPERATIONAL;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.LocalizedString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.transit.model.basic.LocalizedMoney;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vehicle parking updater class for the Norwegian bike box provider Bikely:
 * https://www.safebikely.com/
 */
public class BikelyUpdater implements DataSource<VehicleParking> {

  private static final Logger LOG = LoggerFactory.getLogger(BikelyUpdater.class);

  private static final String JSON_PARSE_PATH = "result";
  private static final Currency NOK = Currency.getInstance("NOK");
  private static final ObjectMapper OBJECT_MAPPER = ObjectMappers.ignoringExtraFields();
  private static final ObjectNode POST_PARAMS = OBJECT_MAPPER.createObjectNode()
    .put("groupPins", true)
    .put("lonMin", 0)
    .put("lonMax", 0)
    .put("latMin", 0)
    .put("latMax", 0);
  private final OtpHttpClient httpClient = new OtpHttpClientFactory().create(LOG);
  private final BikelyUpdaterParameters parameters;
  private List<VehicleParking> lots;

  public BikelyUpdater(BikelyUpdaterParameters parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean update() {
    this.lots = httpClient.postJsonAndMap(
      parameters.url(),
      POST_PARAMS,
      Duration.ofSeconds(30),
      parameters.httpHeaders().asMap(),
      is -> {
        try {
          var lots = new ArrayList<VehicleParking>();
          OBJECT_MAPPER.readTree(is)
            .path(JSON_PARSE_PATH)
            .forEach(node -> lots.add(parseElement(node)));

          return lots.stream().filter(Objects::nonNull).toList();
        } catch (Exception e) {
          LOG.error("Could not get Bikely updates", e);
        }

        return List.of();
      }
    );

    return true;
  }

  @Override
  public List<VehicleParking> getUpdates() {
    return List.copyOf(lots);
  }

  @Nullable
  private VehicleParking parseElement(JsonNode jsonNode) {
    if (jsonNode.path("hasStandardParking").asBoolean()) {
      var vehicleParkId = new FeedScopedId(parameters.feedId(), jsonNode.get("id").asText());

      var lat = jsonNode.get("latitude").asDouble();
      var lng = jsonNode.get("longitude").asDouble();
      var coord = new WgsCoordinate(lat, lng);

      var name = new NonLocalizedString(jsonNode.path("name").asText());

      var totalSpots = jsonNode.get("totalStandardSpots").asInt();
      var freeSpots = jsonNode.get("availableStandardSpots").asInt();
      var isUnderMaintenance = jsonNode.get("isInMaintenance").asBoolean();

      LocalizedString note = toNote(jsonNode);

      VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
        builder
          .entranceId(new FeedScopedId(parameters.feedId(), vehicleParkId.getId() + "/entrance"))
          .name(name)
          .coordinate(coord)
          .walkAccessible(true)
          .carAccessible(false);

      return VehicleParking.builder()
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
    } else {
      return null;
    }
  }

  private static LocalizedString toNote(JsonNode price) {
    var startPriceAmount = price.get("startPriceAmount").floatValue();
    var mainPriceAmount = price.get("mainPriceAmount").floatValue();

    var startPriceDurationHours = price.get("startPriceDuration").asInt();
    var mainPriceDurationHours = price.get("mainPriceDuration").asInt();

    if (startPriceAmount == 0 && mainPriceAmount == 0) {
      return new LocalizedString("price.free");
    } else {
      return new LocalizedString(
        "price.startMain",
        NonLocalizedString.ofNumber(startPriceDurationHours),
        new LocalizedMoney(Money.ofFractionalAmount(NOK, startPriceAmount)),
        new LocalizedMoney(Money.ofFractionalAmount(NOK, mainPriceAmount)),
        NonLocalizedString.ofNumber(mainPriceDurationHours)
      );
    }
  }

  private static VehicleParkingState toState(boolean isUnderMaintenance) {
    if (isUnderMaintenance) return VehicleParkingState.TEMPORARILY_CLOSED;
    else return OPERATIONAL;
  }
}
