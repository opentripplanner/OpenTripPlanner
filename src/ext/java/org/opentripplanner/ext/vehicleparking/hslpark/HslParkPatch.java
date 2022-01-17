package org.opentripplanner.ext.vehicleparking.hslpark;

import org.opentripplanner.model.FeedScopedId;

/**
 * Contains updates to a {@link HslParkUpdater} park.
 */
public class HslParkPatch {

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
}
