package org.opentripplanner.util;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

import java.io.Serializable;

/**
 * This class calculates borders of envelopes that can be also on 180th meridian
 * The same way as it was previously calculated in GraphMetadata constructor
 *
 */
public class WorldEnvelope implements Serializable {

    Envelope leftEnv;
    Envelope rightEnv;

    double aRightCoordinate;

    private double lowerLeftLongitude;
    private double lowerLeftLatitude;
    private double upperRightLongitude;
    private double upperRightLatitude;

    boolean coordinatesCalculated = false;

    public WorldEnvelope() {
        this.leftEnv = new Envelope();
        this.rightEnv = new Envelope();
        this.aRightCoordinate = 0;
    }

    public WorldEnvelope(WorldEnvelope envelope) {
        this.leftEnv = envelope.leftEnv;
        this.rightEnv = envelope.rightEnv;
        this.aRightCoordinate = envelope.aRightCoordinate;
        this.coordinatesCalculated = false;
    }

    public void expandToInclude(Coordinate c) {
        this.expandToInclude(c.x, c.y);
    }

    public void expandToInclude(double x, double y) {
        if (x < 0) {
            leftEnv.expandToInclude(x, y);
        } else {
            rightEnv.expandToInclude(x, y);
            aRightCoordinate = x;
        }
    }

    /**
     * Calculates lower/upper right/left latitude and longitude of all the coordintes
     *
     * This takes into account that envelope can extends over 180th meridian
     */
    private void calculateCoordinates() {
        if (coordinatesCalculated) {
            return;
        }

        if (this.leftEnv.getArea() == 0) {
            //the entire area is in the eastern hemisphere
            this.lowerLeftLongitude = rightEnv.getMinX();
            this.upperRightLongitude = rightEnv.getMaxX();
            this.lowerLeftLatitude = rightEnv.getMinY();
            this.upperRightLatitude = rightEnv.getMaxY();
        } else if (this.rightEnv.getArea() == 0) {
            //the entire area is in the western hemisphere
            this.lowerLeftLongitude = leftEnv.getMinX();
            this.upperRightLongitude = leftEnv.getMaxX();
            this.lowerLeftLatitude = leftEnv.getMinY();
            this.upperRightLatitude = leftEnv.getMaxY();
        } else {
            //the area spans two hemispheres.  Either it crosses the prime meridian,
            //or it crosses the 180th meridian (roughly, the international date line).  We'll check a random
            //coordinate to find out

            if (aRightCoordinate < 90) {
                //assume prime meridian
                this.lowerLeftLongitude = leftEnv.getMinX();
                this.upperRightLongitude = rightEnv.getMaxX();
            } else {
                //assume 180th meridian
                this.lowerLeftLongitude = leftEnv.getMaxX();
                this.upperRightLongitude = rightEnv.getMinX();
            }
            this.upperRightLatitude = Math.max(rightEnv.getMaxY(), leftEnv.getMaxY());
            this.lowerLeftLatitude = Math.min(rightEnv.getMinY(), leftEnv.getMinY());
        }
        coordinatesCalculated = true;
    }

    public double getLowerLeftLongitude() {
        calculateCoordinates();
        return lowerLeftLongitude;
    }

    public double getLowerLeftLatitude() {
        calculateCoordinates();
        return lowerLeftLatitude;
    }

    public double getUpperRightLongitude() {
        calculateCoordinates();
        return upperRightLongitude;
    }

    public double getUpperRightLatitude() {
        calculateCoordinates();
        return upperRightLatitude;
    }

    public boolean contains(Coordinate c) {
        if (c.x < 0) {
            return leftEnv.contains(c);
        } else {
            return rightEnv.contains(c);
        }
    }
}