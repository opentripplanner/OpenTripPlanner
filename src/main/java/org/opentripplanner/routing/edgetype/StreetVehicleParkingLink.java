package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;
import org.opentripplanner.util.I18NString;

/**
 * This represents the connection between a street vertex and a vehicle parking vertex.
 */
public class StreetVehicleParkingLink extends Edge {

    private final VehicleParkingEntranceVertex vehicleParkingEntranceVertex;

    public StreetVehicleParkingLink(StreetVertex fromv, VehicleParkingEntranceVertex tov) {
        super(fromv, tov);
        vehicleParkingEntranceVertex = tov;
    }

    public StreetVehicleParkingLink(VehicleParkingEntranceVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        vehicleParkingEntranceVertex = fromv;
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

    public I18NString getName() {
        return vehicleParkingEntranceVertex.getName();
    }

    public State traverse(State s0) {
        final var options = s0.getOptions();

        // Disallow traversing two StreetBikeParkLinks in a row.
        // Prevents router using bike rental stations as shortcuts to get around
        // turn restrictions.
        if (s0.getBackEdge() instanceof StreetVehicleParkingLink) {
            return null;
        }

        var entrance = vehicleParkingEntranceVertex.getParkingEntrance();
        if (s0.getNonTransitMode() == TraverseMode.CAR) {
            if (!entrance.isCarAccessible()) {
                return null;
            }
        }
        else if (!entrance.isWalkAccessible()) {
            return null;
        }

        var vehicleParking = vehicleParkingEntranceVertex.getVehicleParking();
        if (hasMissingRequiredTags(options, vehicleParking) || hasBannedTags(options, vehicleParking)) {
            return null;
        }

        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(1);
        s1.setBackMode(null);
        return s1.makeState();
    }

    private boolean hasBannedTags(RoutingRequest options, VehicleParking vehicleParking) {
        if (options.bannedVehicleParkingTags.isEmpty()) {
            return false;
        }

        return vehicleParking.getTags().stream().anyMatch(options.bannedVehicleParkingTags::contains);
    }

    private boolean hasMissingRequiredTags(RoutingRequest options, VehicleParking vehicleParking) {
        if (options.requiredVehicleParkingTags.isEmpty()) {
            return false;
        }

        return !vehicleParking.getTags().containsAll(options.requiredVehicleParkingTags);
    }

    public String toString() {
        return "StreetVehicleParkingLink(" + fromv + " -> " + tov + ")";
    }
}
