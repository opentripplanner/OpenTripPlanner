package org.opentripplanner.analyst.batch;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.opentripplanner.analyst.core.Sample;

/** Individual locations that make up Populations for the purpose of many-to-many searches. */
@ToString @RequiredArgsConstructor
public class Individual {

    public final String label;
    public final double lon;
    public final double lat;
    @NonNull public double input;  // not final to allow clamping and scaling by filters
    public Sample sample= null; // not final, allowing sampling to occur after filterings
    
    // public boolean rejected;
        
}
