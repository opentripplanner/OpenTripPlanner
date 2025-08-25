package org.opentripplanner.updater.trip.gtfs;

import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.updater.trip.gtfs.model.StopTimeUpdate;

/**
 * A stop time update paired with its stop, constructed while validating the stops
 */
record StopAndStopTimeUpdate(StopLocation stop, StopTimeUpdate stopTimeUpdate) {}
