package org.opentripplanner.model;

import java.util.Objects;
import org.opentripplanner.model.calendar.ServiceDate;

/**
 * Class to use as key in HashMap containing feed id, trip id and service date
 */
public record TripIdAndServiceDate(FeedScopedId tripId, ServiceDate serviceDate) {}
