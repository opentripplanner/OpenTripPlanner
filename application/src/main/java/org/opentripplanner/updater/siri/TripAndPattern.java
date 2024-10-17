package org.opentripplanner.updater.siri;

import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

record TripAndPattern(Trip trip, TripPattern tripPattern) {}
