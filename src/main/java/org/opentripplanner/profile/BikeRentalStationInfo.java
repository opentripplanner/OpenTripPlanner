package org.opentripplanner.profile;

import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;

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
