package org.opentripplanner.analyst.batch;

import org.opentripplanner.analyst.core.Sample;

/**
 * Individual locations that make up Populations for the purpose
 * of many-to-many searches.
 *  
 * @author andrewbyrd
 *
 */
public class Individual {

    public String  id;
    public Sample  sample;
    public double data;
    private double lon;
    private double lat;
    
    public Individual() {
        // bean constructor
    }
    
    public Individual(String id, Sample sample, double lon, double lat, double data) {
        this.id = id;
        this.sample = sample;
        this.data = data;
        this.setLon(lon);
        this.setLat(lat);
    }
    
    public Population asPopulation() {
        return new Population(this);
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    
}
