package org.opentripplanner.profile;

import org.opentripplanner.analyst.PointSet;

/** Represents a set of distributions at each point in a pointset */
public class DistributionResultSet {
    /** the distribution of travel times at each destination. */ 
    public Distribution[] distributions;
    
    public DistributionResultSet(int capacity) {
        this.distributions = new Distribution[capacity];
    }
    
    public DistributionResultSet(PointSet pset) {
        this(pset.capacity);
    }
}
