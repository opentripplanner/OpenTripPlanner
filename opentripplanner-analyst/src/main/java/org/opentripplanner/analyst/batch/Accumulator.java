package org.opentripplanner.analyst.batch;

public interface Accumulator {

    public void accumulate(double amount, ResultSet current, ResultSet accumulated);
    
    public void finish();
    
}
