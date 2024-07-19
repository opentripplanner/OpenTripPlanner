package org.opentripplanner.updater.vehicle_parking;

import java.util.Objects;
import org.opentripplanner.framework.lang.IntUtils;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public record AvailabiltyUpdate(FeedScopedId vehicleParkingId, int spacesAvailable) {
  public AvailabiltyUpdate {
    Objects.requireNonNull(vehicleParkingId);
    IntUtils.requireNotNegative(spacesAvailable);
  }
}
