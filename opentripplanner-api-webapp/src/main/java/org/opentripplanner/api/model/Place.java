package org.opentripplanner.api.model;

import java.util.logging.Logger;

import javax.xml.bind.annotation.XmlElement;

import org.opentripplanner.util.Constants;
/**
 *
 */
public class Place {

    protected static final Logger LOGGER = Logger.getLogger(Place.class.getCanonicalName());

    public String name = null;

    public String stopId = "123";

    public Double lon = null;
    public Double lat = null;

    @XmlElement
    String getGeometry() {

        return Constants.GEO_JSON + lon + "," + lat + Constants.GEO_JSON_TAIL;
    }

    public Place() {
    }

    public Place(Double lon, Double lat, String name) {
        this.lon = lon;
        this.lat = lat;
        this.name = name;
    }

    public Place(Double lon, Double lat, String name, String stopId) {
        this(lon, lat, name);
        this.stopId = stopId;
    }

}
