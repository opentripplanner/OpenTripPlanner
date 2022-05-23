package org.opentripplanner.model;

import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.transit.model.basic.FeedScopedId;

/**
 * Class to use as key in HashMap containing feed id, trip id and service date
 */
public record TripIdAndServiceDate(FeedScopedId tripId, ServiceDate serviceDate) {}
