package org.opentripplanner.api.parameter;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import javax.ws.rs.BadRequestException;

public class BoundingBox {
    
    double minLat, minLon, maxLat, maxLon;

    public BoundingBox (String s) {
        String[] elements = s.split(","); // what about ; between coordinates?
        if (elements.length != 4) {
            throw new BadRequestException("A bounding box must have four coordinates.");
        }
        try {
            minLat = Double.parseDouble(elements[0]);
            minLon = Double.parseDouble(elements[1]);
            maxLat = Double.parseDouble(elements[2]);
            maxLon = Double.parseDouble(elements[3]);
        } catch (NumberFormatException pe) {
            throw new BadRequestException("Unable to parse coordinate: " + pe.getMessage());
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
