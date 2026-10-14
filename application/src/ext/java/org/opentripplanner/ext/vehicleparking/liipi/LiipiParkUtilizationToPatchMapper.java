package org.opentripplanner.ext.vehicleparking.liipi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

/**
 * Mapper for parsing a utilization into an {@link LiipiParkPatch}.
 */
public class LiipiParkUtilizationToPatchMapper {

  private static final Logger LOG = LoggerFactory.getLogger(
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
      String capacityType = jsonNode.path("capacityType").asString();
      Integer spacesAvailable = LiipiParkToVehicleParkingMapper.parseIntegerValue(
        jsonNode,
        "spacesAvailable"
      );
      return new LiipiParkPatch(vehicleParkId, capacityType, spacesAvailable);
    } catch (Exception e) {
      LOG.warn("Error parsing park utilization {}", vehicleParkId, e);
      return null;
    }
  }
}
