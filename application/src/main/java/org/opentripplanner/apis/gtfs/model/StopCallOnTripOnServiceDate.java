package org.opentripplanner.apis.gtfs.model;

import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;

/**
 * A canceled stop call together with the {@link TripOnServiceDate} it belongs to.
 */
public record StopCallOnTripOnServiceDate(
  TripOnServiceDate tripOnServiceDate,
  TripTimeOnDate stopCall
) {}
