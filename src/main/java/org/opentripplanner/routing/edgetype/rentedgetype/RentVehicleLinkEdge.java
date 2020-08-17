package org.opentripplanner.routing.edgetype.rentedgetype;

import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleSplitterVertex;
import org.opentripplanner.routing.vertextype.TemporaryRentVehicleVertex;

/**
 * This edge links vertex from a graph (or {@link TemporaryRentVehicleSplitterVertex})
 * with {@link TemporaryRentVehicleVertex}, and vice versa.
 */
public class RentVehicleLinkEdge extends FreeEdge implements TemporaryEdge {

    public RentVehicleLinkEdge(Vertex from, Vertex to) {
        super(from, to);
    }
}
