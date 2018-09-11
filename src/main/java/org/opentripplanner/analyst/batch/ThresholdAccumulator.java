package org.opentripplanner.analyst.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThresholdAccumulator implements Accumulator {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdAccumulator.class);

    public int thresholdSeconds = 60 * 90; // 1.5 hours in seconds

    public void setThresholdMinutes(int minutes) {
        this.thresholdSeconds = minutes * 60;
    }
    
    @Override
    public void accumulate(double amount, ResultSet current, ResultSet accumulated) {
        if (current.population != accumulated.population) {
            return;
        }
        int n = accumulated.population.size();
        for (int i = 0; i < n; i++) {
            double t = current.results[i]; 
            if (t > 0 && t < thresholdSeconds) {
                accumulated.results[i] += amount;
            }
        }
    }

    @Override
    public void finish() {
        // nothing to do
    }

}
