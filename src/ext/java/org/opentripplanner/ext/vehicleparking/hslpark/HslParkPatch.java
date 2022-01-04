package org.opentripplanner.ext.vehicleparking.hslpark;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.model.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains updates to a {@link HslParkUpdater} park and a method for parsing a utilization into an HslParkPatch.
 */
public class HslParkPatch {

    private static final Logger log = LoggerFactory.getLogger(HslParkPatch.class);

    private final FeedScopedId facilityId;
    private final String capacityType;
    private final Integer spacesAvailable;

    public HslParkPatch(
            FeedScopedId facilityId,
            String capacityType,
            Integer spacesAvailable
    ) {
        this.facilityId = facilityId;
        this.capacityType = capacityType;
        this.spacesAvailable = spacesAvailable;
    }

    public FeedScopedId getId() {
        return this.facilityId;
    }

    public String getCapacityType() {
        return this.capacityType;
    }

    public Integer getSpacesAvailable() {
        return this.spacesAvailable;
    }

    public static HslParkPatch parseUtilization(JsonNode jsonNode) {
        var vehicleParkId = HslParkToVehicleParkingMapper.createIdForNode(jsonNode, "facilityId");
        try {
            String capacityType = jsonNode.path("capacityType").asText();
            Integer spacesAvailable = HslParkToVehicleParkingMapper.parseIntegerValue(jsonNode, "spacesAvailable");
            return new HslParkPatch(vehicleParkId, capacityType, spacesAvailable);
        }
        catch (Exception e) {
            log.warn("Error parsing park utilization" + vehicleParkId, e);
            return null;
        }
    }
}
