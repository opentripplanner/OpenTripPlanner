package org.opentripplanner.routing.edgetype;

import java.util.Locale;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

/**
 * This represents the connection between a street vertex and a vehicle parking vertex.
 */
public class StreetVehicleParkingLink extends Edge {

    private final VehicleParkingVertex vehicleParkingVertex;

    public StreetVehicleParkingLink(StreetVertex fromv, VehicleParkingVertex tov) {
        super(fromv, tov);
        vehicleParkingVertex = tov;
    }

    public StreetVehicleParkingLink(VehicleParkingVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        vehicleParkingVertex = fromv;
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

    public String getName() {
        return vehicleParkingVertex.getName();
    }

    public String getName(Locale locale) {
        return vehicleParkingVertex.getName(locale);
    }

    public State traverse(State s0) {
        // Disallow traversing two StreetBikeParkLinks in a row.
        // Prevents router using bike rental stations as shortcuts to get around
        // turn restrictions.
        if (s0.getBackEdge() instanceof StreetVehicleParkingLink) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(1);
        s1.setBackMode(null);
        return s1.makeState();
    }

    public String toString() {
        return "StreetVehicleParkingLink(" + fromv + " -> " + tov + ")";
    }
}
