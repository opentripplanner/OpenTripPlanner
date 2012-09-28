package org.opentripplanner.analyst.batch;

import lombok.Data;
import lombok.Setter;

@Data
public class InputClampFilter implements IndividualFilter {

    public double rejectMin = 0;
    private double rejectMax = Double.MAX_VALUE;
    public double clampMin = 0;
    public double clampMax = Double.MAX_VALUE;
    
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
