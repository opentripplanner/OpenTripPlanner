package org.opentripplanner.ext.vehicleparking.liipi;

import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Contains updates to a {@link LiipiParkUpdater} park.
 */
public class LiipiParkPatch {

  private final FeedScopedId facilityId;
  private final String capacityType;
  private final Integer spacesAvailable;

  public LiipiParkPatch(FeedScopedId facilityId, String capacityType, Integer spacesAvailable) {
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
