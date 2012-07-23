package org.opentripplanner.analyst.batch;

import lombok.Data;

import org.opentripplanner.analyst.core.Sample;

/**
 * Individual locations that make up Populations for the purpose
 * of many-to-many searches.
 */
@Data
public class Individual {

    public String label;
    public double lon;
    public double lat;
    public Sample sample;
    public double input;
        
    public Individual() {
    	
    }
    
    public Individual(String label, Sample sample, double lon, double lat, double input) {
        this.label = label;
        this.sample = sample;
        this.input = input;
        this.setLon(lon);
        this.setLat(lat);
    }
}
