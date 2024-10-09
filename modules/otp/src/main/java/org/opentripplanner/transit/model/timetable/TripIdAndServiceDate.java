package org.opentripplanner.transit.model.timetable;

import java.time.LocalDate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Class to use as key in HashMap containing feed id, trip id and service date
 */
public record TripIdAndServiceDate(FeedScopedId tripId, LocalDate serviceDate) {}
