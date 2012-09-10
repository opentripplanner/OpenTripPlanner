package org.opentripplanner.analyst.batch;

import lombok.Data;
import lombok.Setter;

@Data
public class InputClampFilter implements IndividualFilter {

    @Setter public double rejectMin = 0;
    @Setter private double rejectMax = Double.MAX_VALUE;
    @Setter public double clampMin = 0;
    @Setter public double clampMax = Double.MAX_VALUE;
    
    @Override
    public boolean filter(Individual individual) {
        double input = individual.input;
        if (input < rejectMin || input > rejectMax)
            return false;
        if (input < clampMin)
            input = clampMin;
        if (input > clampMax)
            input = clampMax;
        return true;
    }

}
