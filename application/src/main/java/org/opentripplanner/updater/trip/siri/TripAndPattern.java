package org.opentripplanner.updater.trip.siri;

import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

record TripAndPattern(Trip trip, TripPattern tripPattern) {}
