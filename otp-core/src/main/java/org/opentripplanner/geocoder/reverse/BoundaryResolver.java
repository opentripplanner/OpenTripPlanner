package org.opentripplanner.geocoder.reverse;

public interface BoundaryResolver {
    
    public String resolve(double lat, double lon);

}
