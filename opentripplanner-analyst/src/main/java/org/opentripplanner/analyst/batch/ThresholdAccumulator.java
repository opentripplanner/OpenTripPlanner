package org.opentripplanner.analyst.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Setter;
public class ThresholdAccumulator implements Accumulator {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdAccumulator.class);

    @Setter 
    int threshold = 60 * 90; // 1.5 hours in seconds

    @Override
    public void accumulate(double amount, ResultSet current, ResultSet accumulated) {
        if (current.population != accumulated.population) {
            return;
        }
        int n = accumulated.population.size();
        for (int i = 0; i < n; i++) {
            double t = current.results[i]; 
            if (t > 0 && t < threshold) {
                accumulated.results[i] += amount;
            }
        }
    }

    @Override
    public void finish() {
        // nothing to do
    }

}
