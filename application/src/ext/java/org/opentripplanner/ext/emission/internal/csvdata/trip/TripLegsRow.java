package org.opentripplanner.ext.emission.internal.csvdata.trip;

import org.opentripplanner.framework.model.Gram;

record TripLegsRow(String tripId, String fromStopId, int fromStopSequence, Gram co2) {}
