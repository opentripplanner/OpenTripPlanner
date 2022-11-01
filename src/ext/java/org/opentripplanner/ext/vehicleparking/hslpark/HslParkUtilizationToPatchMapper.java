package org.opentripplanner.ext.vehicleparking.hslpark;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mapper for parsing a utilization into an {@link HslParkPatch}.
 */
public class HslParkUtilizationToPatchMapper {

  private static final Logger log = LoggerFactory.getLogger(HslParkUtilizationToPatchMapper.class);

  private final String feedId;

  public HslParkUtilizationToPatchMapper(String feedId) {
    this.feedId = feedId;
  }

  public HslParkPatch parseUtilization(JsonNode jsonNode) {
    var vehicleParkId = HslParkToVehicleParkingMapper.createIdForNode(
      jsonNode,
      "facilityId",
      feedId
    );
    try {
      String capacityType = jsonNode.path("capacityType").asText();
      Integer spacesAvailable = HslParkToVehicleParkingMapper.parseIntegerValue(
        jsonNode,
        "spacesAvailable"
      );
      return new HslParkPatch(vehicleParkId, capacityType, spacesAvailable);
    } catch (Exception e) {
      log.warn("Error parsing park utilization {}", vehicleParkId, e);
      return null;
    }
  }
}
