package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;
import org.opentripplanner.util.I18NString;

/**
 * This represents the connection between a street vertex and a bike rental station vertex.
 * 
 */
public class StreetVehicleRentalLink extends Edge {

    private static final long serialVersionUID = 1L;

    private VehicleRentalStationVertex vehicleRentalStationVertex;

    public StreetVehicleRentalLink(StreetVertex fromv, VehicleRentalStationVertex tov) {
        super(fromv, tov);
        vehicleRentalStationVertex = tov;
    }

    public StreetVehicleRentalLink(VehicleRentalStationVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        vehicleRentalStationVertex = fromv;
    }

    public String getDirection() {
        return null;
    }

    public double getDistanceMeters() {
        return 0;
    }

    public LineString getGeometry() {
        return null;
    }

    @Override
    public I18NString getName() {
        return vehicleRentalStationVertex.getName();
    }

    public State traverse(State s0) {
        // Disallow traversing two StreetBikeRentalLinks in a row.
        // This prevents the router from using bike rental stations as shortcuts to get around
        // turn restrictions.
        if (s0.getBackEdge() instanceof StreetVehicleRentalLink) {
            return null;
        }

        if (vehicleRentalStationVertex.getStation().networkIsNotAllowed(s0.getOptions())) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        //assume bike rental stations are more-or-less on-street
        s1.incrementWeight(1);
        s1.setBackMode(null);
        return s1.makeState();
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }

    public String toString() {
        return "StreetVehicleRentalLink(" + fromv + " -> " + tov + ")";
    }
}
