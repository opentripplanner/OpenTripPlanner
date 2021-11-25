package org.opentripplanner.model;

import org.opentripplanner.routing.vertextype.VehicleRentalStationVertex;

/**
 * This is a response model class which holds data that will be serialized and returned to the client.
 * It is not used internally in routing.
 */
public class VehicleRentalStationInfo {

    public String id;
    public String name;
    public Double lat, lon;
    
    public VehicleRentalStationInfo(VehicleRentalStationVertex vertex) {
        id = vertex.getStation().getStationId();
        name = vertex.getName();
        lat = vertex.getLat();
        lon = vertex.getLon();
    }
}
