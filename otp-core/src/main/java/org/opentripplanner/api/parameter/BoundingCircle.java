package org.opentripplanner.api.parameter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class BoundingCircle {
    
    double lat, lon, radius;

    private static final double METERS_PER_DEGREE_LAT = 111111;
    private static final double MAX_RADIUS = 10000; // meters
            
    private static void err (String message) {
        throw new WebApplicationException(Response
                .status(Status.BAD_REQUEST)
                .entity(message)
                .build());
    }
    
    public BoundingCircle (String s) {
        String[] elements = s.split(","); // what about ; between coordinates?
        if (elements.length != 3) {
            err ("A circle must have two coordinates and a radius.");
        }
        try {
            lat    = Double.parseDouble(elements[0]);
            lon    = Double.parseDouble(elements[1]);
            radius = Double.parseDouble(elements[2]);
        } catch (NumberFormatException pe) {
            err ("Unable to parse coordinate: " + pe.getMessage());
        }        
        if (radius < 0 || radius > MAX_RADIUS) {
            err ("Radius out of range.");
        }
        // check that lat/lon are in range as well, using shared geographic range check function
    }
    
    public Coordinate center () {
        return new Coordinate(lon, lat);
    }

    public Envelope bbox () {
        Envelope env = new Envelope(center());
        double meters_per_degree_lon = METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(lat));
        env.expandBy(radius / meters_per_degree_lon, radius / METERS_PER_DEGREE_LAT);
        return env;
    }

}
