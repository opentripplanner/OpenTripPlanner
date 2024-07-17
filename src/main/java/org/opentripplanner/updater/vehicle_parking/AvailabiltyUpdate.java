package org.opentripplanner.updater.vehicle_parking;

import org.opentripplanner.transit.model.framework.FeedScopedId;

public record AvailabiltyUpdate(FeedScopedId vehicleParkingId, int spacesAvailable) {}
