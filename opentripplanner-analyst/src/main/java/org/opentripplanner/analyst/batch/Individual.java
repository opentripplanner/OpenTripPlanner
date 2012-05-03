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

    public final String  id;
    public final Sample  sample;
    public final double data;
    public final double lon, lat;
    
    public Individual(String id, Sample sample, double lon, double lat, double data) {
        this.id = id;
        this.sample = sample;
        this.data = data;
        this.lon = lon;
        this.lat = lat;
    }
    
    public Population asPopulation() {
        return new Population(this);
    }
    
}
