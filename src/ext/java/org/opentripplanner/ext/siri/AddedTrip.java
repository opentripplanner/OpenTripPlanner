package org.opentripplanner.ext.siri;

import java.time.LocalDate;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

record AddedTrip(Trip trip, StopPattern stopPattern, TripTimes tripTimes, LocalDate serviceDate) {}
