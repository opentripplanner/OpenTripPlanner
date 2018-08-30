package org.opentripplanner.analyst.batch;

import org.opentripplanner.analyst.core.Sample;

/** Individual locations that make up Populations for the purpose of many-to-many searches. */
public class Individual {

    public String label;
    public double lon;
    public double lat;
    public double input;  // not final to allow clamping and scaling by filters
    public Sample sample= null; // not final, allowing sampling to occur after filterings
    
    public Individual(String label, double lon, double lat, double input) {
        this.label = label;
        this.lon = lon;
        this.lat = lat;
        this.input = input;
    }

    public Individual() { }
 
    // public boolean rejected;

    
}
