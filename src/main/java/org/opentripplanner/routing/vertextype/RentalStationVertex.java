package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vehicle_rental.RentalStation;
import org.opentripplanner.util.I18NString;

/**
 * An interface for modelling all possible vehicle rental vertices.
 */
public abstract class RentalStationVertex extends Vertex {
    // this is approximately 5 meters (https://gis.stackexchange.com/a/8655/12590)
    private static final double DIFFERENT_POSITION_COORDINATE_EPSILON = 0.00005;

    protected RentalStationVertex(Graph g, String label, double x, double y, I18NString name) {
        super(g, label, x, y, name);
    }

    /**
     * A simple check if a rental station vertex's position is different than another rental station's coordinates.
     */
    public boolean hasDifferentApproximatePosition(RentalStation otherStation) {
        return Math.abs(otherStation.x - getX()) > DIFFERENT_POSITION_COORDINATE_EPSILON ||
            Math.abs(otherStation.y - getY()) > DIFFERENT_POSITION_COORDINATE_EPSILON;
    }
}
