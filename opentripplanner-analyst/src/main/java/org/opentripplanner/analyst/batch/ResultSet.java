package org.opentripplanner.analyst.batch;

import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// store output outside individuals so populations can be reused
public class ResultSet {

    private static final Logger LOG = LoggerFactory.getLogger(ResultSet.class);

    public Population population;
    public double[] results;
    
    public static ResultSet forTravelTimes(Population population, ShortestPathTree spt) {
        double[] results = new double[population.size()];
        int i = 0;
        for (Individual indiv : population) {
            Sample s = indiv.sample;
            long t = Long.MAX_VALUE;
            if (s == null)
                t = -2;
            else
                t = s.eval(spt);
            if (t == Long.MAX_VALUE)
                t = -1;
            results[i] = t;
            i++;
        }
        return new ResultSet(population, results);
    }
    
    public ResultSet(Population population, double[] results) {
        this.population = population;
        this.results = results;
    }
    
    protected ResultSet(Population population) {
        this.population = population;
        this.results = new double[population.size()];
    }

    public void writeAppropriateFormat(String outFileName) {
        population.writeAppropriateFormat(outFileName, this);
    }
    
}
