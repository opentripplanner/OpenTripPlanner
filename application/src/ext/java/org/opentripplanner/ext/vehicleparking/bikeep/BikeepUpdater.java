package org.opentripplanner.ext.vehicleparking.bikeep;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import java.util.List;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.json.ObjectMappers;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingSpaces;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.updater.spi.GenericJsonDataSource;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Vehicle parking updater for Bikeep's API.
 */
public class BikeepUpdater extends GenericJsonDataSource<VehicleParking> {

  private static final String JSON_PARSE_PATH = "features";
  private static final ObjectReader STRING_LIST_READER = ObjectMappers.ignoringExtraFields()
    .readerForListOf(String.class);
  private final BikeepUpdaterParameters params;

  public BikeepUpdater(BikeepUpdaterParameters parameters) {
    super(parameters.url().toString(), JSON_PARSE_PATH, parameters.httpHeaders());
    this.params = parameters;
  }

  @Override
  protected VehicleParking parseElement(JsonNode jsonNode) {
    try {
      var coords = jsonNode.path("geometry").path("coordinates");
      var coordinate = new WgsCoordinate(coords.get(1).asDouble(), coords.get(0).asDouble());

      var props = jsonNode.path("properties");
      var vehicleParkId = new FeedScopedId(params.feedId(), props.path("code").asText());
      var name = new NonLocalizedString(props.path("label").asText());
      var parking = props.path("parking");

      List<String> tags = STRING_LIST_READER.readValue(props.path("tags"));

      var availability = VehicleParkingSpaces.builder()
        .bicycleSpaces(parking.get("available").asInt())
        .build();
      var capacity = VehicleParkingSpaces.builder()
        .bicycleSpaces(parking.get("total").asInt())
        .build();

      VehicleParking.VehicleParkingEntranceCreator entrance = builder ->
        builder
          .entranceId(new FeedScopedId(params.feedId(), vehicleParkId.getId() + "/entrance"))
          .coordinate(coordinate)
          .walkAccessible(true)
          .carAccessible(true);

      return VehicleParking.builder()
        .id(vehicleParkId)
        .name(name)
        .state(VehicleParkingState.OPERATIONAL)
        .coordinate(coordinate)
        .bicyclePlaces(true)
        .availability(availability)
        .tags(tags)
        .capacity(capacity)
        .entrance(entrance)
        .build();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(this.getClass())
      .addStr("feedId", this.params.feedId())
      .addStr("url", this.params.url().toString())
      .toString();
  }
}
