package org.opentripplanner.api.parameter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class BoundingBox {
    
    double minLat, minLon, maxLat, maxLon;

    private static void err (String message) {
        throw new WebApplicationException(Response
                .status(Status.BAD_REQUEST)
                .entity(message)
                .build());
    }
    
    public BoundingBox (String s) {
        String[] elements = s.split(","); // what about ; between coordinates?
        if (elements.length != 4) {
            err ("A bounding box must have four coordinates.");
        }
        try {
            minLat = Double.parseDouble(elements[0]);
            minLon = Double.parseDouble(elements[1]);
            maxLat = Double.parseDouble(elements[2]);
            maxLon = Double.parseDouble(elements[3]);
        } catch (NumberFormatException pe) {
            err ("Unable to parse coordinate: " + pe.getMessage());
        }        
    }
    
    public Coordinate lowerLeft () {
        return new Coordinate(minLon, minLat);
    }

    public Coordinate upperLeft () {
        return new Coordinate(minLon, maxLat);
    }

    public Coordinate lowerRight () {
        return new Coordinate(maxLon, minLat);
    }

    public Coordinate upperRight () {
        return new Coordinate(maxLon, maxLat);
    }
    
    public Envelope envelope () {
        return new Envelope(minLon,  maxLon, minLat, maxLat);
    }

}
