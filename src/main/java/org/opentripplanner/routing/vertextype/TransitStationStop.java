package org.opentripplanner.routing.vertextype;

import org.opentripplanner.model.Stop;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.graph.Graph;

/**
 * Common abstract superclass for Stations and Stops.
 * They come from the same table in GTFS, but we want to distinguish between them.
 */
public abstract class TransitStationStop extends OffboardVertex {
    private static final long serialVersionUID = 1L;

    public TransitStationStop(Graph graph, Stop stop) {
        super(graph, GtfsLibrary.convertIdToString(stop.getId()), stop);
    }
}
