package org.opentripplanner.updater.vehicle_parking;

import java.util.Objects;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.IntUtils;

public record AvailabiltyUpdate(FeedScopedId vehicleParkingId, int spacesAvailable) {
  public AvailabiltyUpdate {
    Objects.requireNonNull(vehicleParkingId);
    IntUtils.requireNotNegative(spacesAvailable);
  }
}
