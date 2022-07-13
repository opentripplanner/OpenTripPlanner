package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.transit.model.timetable.Trip;

public record StaySeatedNotAllowed(Trip fromTrip, Trip toTrip) {}
