package org.opentripplanner.updater.trip;

import org.opentripplanner.transit.model.site.RegularStop;

record StopCall(RegularStop stop, int arrivalTime, int departureTime) {}
