package org.opentripplanner.transit.model.timetable;

import java.time.LocalDate;

/**
 * Class which represents a trin on a specific date
 */
public record DatedTrip(Trip trip, LocalDate serviceDate) {}
