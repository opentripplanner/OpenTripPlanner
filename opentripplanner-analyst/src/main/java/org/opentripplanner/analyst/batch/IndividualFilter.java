package org.opentripplanner.analyst.batch;

public interface IndividualFilter {

    /**
     * An interface for classes that examine each Individual in a Population before a batch analyst run.
     * IndividualFilters can modify the individuals (for example, clamping or transforming values)
     * and accept or reject them for use in the analysis.
     * 
     * @param individual
     * @return true if the inividual should be included in the analysis, false otherwise
     */
    public boolean filter(Individual individual);
    
}
