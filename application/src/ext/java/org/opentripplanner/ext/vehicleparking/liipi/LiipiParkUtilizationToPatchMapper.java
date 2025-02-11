package org.opentripplanner.ext.vehicleparking.liipi;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper for parsing a utilization into an {@link LiipiParkPatch}.
 */
public class LiipiParkUtilizationToPatchMapper {

  private static final Logger log = LoggerFactory.getLogger(
    LiipiParkUtilizationToPatchMapper.class
  );

  private final String feedId;

  public LiipiParkUtilizationToPatchMapper(String feedId) {
    this.feedId = feedId;
  }

  public LiipiParkPatch parseUtilization(JsonNode jsonNode) {
    var vehicleParkId = LiipiParkToVehicleParkingMapper.createIdForNode(
      jsonNode,
      "facilityId",
      feedId
    );
    try {
      String capacityType = jsonNode.path("capacityType").asText();
      Integer spacesAvailable = LiipiParkToVehicleParkingMapper.parseIntegerValue(
        jsonNode,
        "spacesAvailable"
      );
      return new LiipiParkPatch(vehicleParkId, capacityType, spacesAvailable);
    } catch (Exception e) {
      log.warn("Error parsing park utilization {}", vehicleParkId, e);
      return null;
    }
  }
}
