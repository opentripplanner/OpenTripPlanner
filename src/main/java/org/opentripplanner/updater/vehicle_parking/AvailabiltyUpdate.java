package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public sealed interface AvailabiltyUpdate {
  FeedScopedId vehicleParkingId();

  record AvailabilityUpdated(FeedScopedId vehicleParkingId, int spacesAvailable)
    implements AvailabiltyUpdate {}

  record ParkingClosed(FeedScopedId vehicleParkingId) implements AvailabiltyUpdate {}
}
