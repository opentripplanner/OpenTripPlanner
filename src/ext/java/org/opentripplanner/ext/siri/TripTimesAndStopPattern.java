package org.opentripplanner.ext.siri;

import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

public record TripTimesAndStopPattern(TripTimes times, StopPattern pattern) {}
