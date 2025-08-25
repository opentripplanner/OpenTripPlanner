package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;

record TripTimesWithStopPattern(RealTimeTripTimes tripTimes, StopPattern stopPattern) {}
