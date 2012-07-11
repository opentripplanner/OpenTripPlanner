package org.opentripplanner.routing.edgetype;

import java.util.List;

import org.onebusaway.gtfs.model.Stop;

/* simple interface for trip patterns */
public interface TripPattern {
    List<Stop> getStops();
}