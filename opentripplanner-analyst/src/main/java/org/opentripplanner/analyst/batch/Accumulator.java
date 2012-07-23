package org.opentripplanner.analyst.batch;

import org.opentripplanner.routing.spt.ShortestPathTree;

public interface Accumulator {

    public void accumulate(double amount, ResultSet current, ResultSet accumulated);
    
    public void finish();
    
}
