package org.opentripplanner.profile;

import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;

/**
 * This is a response model class which holds data that will be serialized and returned to the client.
 * It is not used internally in routing.
 */
public class BikeRentalStationInfo {

    public String id;
    public String name;
    public Double lat, lon;
    
    public BikeRentalStationInfo(BikeRentalStationVertex vertex) {
        id = vertex.getId();
        name = vertex.getName();
        lat = vertex.getLat();
        lon = vertex.getLon();
    }
}
