package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.StreetEdge;

/**
 * Given street has been split temporarily in this location, to allow linking rentable vehicle.
 */
public class TemporaryRentVehicleSplitterVertex extends TemporarySplitterVertex {

    public TemporaryRentVehicleSplitterVertex(String label, double x, double y, StreetEdge streetEdge) {
        super(label, x, y, streetEdge, false);
    }
}
