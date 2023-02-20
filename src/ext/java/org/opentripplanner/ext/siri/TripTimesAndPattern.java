package org.opentripplanner.ext.siri;

import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

public record TripTimesAndPattern(TripTimes times, TripPattern pattern) {}
