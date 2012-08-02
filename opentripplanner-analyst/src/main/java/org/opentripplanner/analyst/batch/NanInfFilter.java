package org.opentripplanner.analyst.batch;

import lombok.Data;

@Data
public class NanInfFilter implements IndividualFilter {

    private boolean rejectInfinite = true;
    private boolean rejectNan = true;
    private double replaceInfiniteWith = 0;
    private double replaceNanWith = 0;
    
    @Override
    public boolean filter(Individual individual) {
        double input = individual.input;
        if (Double.isInfinite(input)) {
            if (rejectInfinite) {
                return false;
            } else {
                individual.input = replaceInfiniteWith;
            }
        } else if (Double.isNaN(input)) {
            if (rejectNan) {
                return false;
            } else {
                individual.input = replaceNanWith;
            }
        }
        return true;
    }

}
