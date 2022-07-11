package org.opentripplanner.model.impl;

import org.opentripplanner.transit.model.timetable.Trip;

public record StaySeatedNotAllowed(Trip fromTrip, Trip toTrip) {}
