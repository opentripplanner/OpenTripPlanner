package org.opentripplanner.analyst;

import java.util.Map.Entry;

/**
 * Represents the difference between two Analyst ResultSets. This expresses the change in travel time from one point to
 * a whole set of points (improvement or worsening) when switching from one transport network scenario to another.
 *
 * TODO since the times2 are pulled out of another ResultSet I'd really expect this class to be a wrapper around two ResultSet instances.
 */
public class ResultSetDelta extends ResultSet {

    private static final long serialVersionUID = -6723127825189535112L;

    public int[] times2;
    public int[] delta;

    public ResultSetDelta(SampleSet samples1, SampleSet samples2, TimeSurface surface1, TimeSurface surface2) {
        id = samples1.pset.id + "_" + surface1.id + "_" + surface2.id + "_delta";

        // Evaluate the surface at all points in the pointset
        this.times = samples1.eval(surface1);
        this.times2 = samples2.eval(surface2);

        buildDelta();

        buildDeltaHistograms(samples1.pset);
    }
    
    /** build a resultsetdelta from two resultsetswithtimes that have already been precalculated */
    public ResultSetDelta(ResultSet result1, ResultSet result2) {
        if (result1.times.length != result2.times.length)
            throw new IllegalArgumentException("Result sets do not match when constructing delta!");
        
        this.times = result1.times;
        this.times2 = result2.times;
        
        buildDelta();
    }
    
    protected void buildDelta () {
        this.delta = new int[times.length];

        for(int i = 0; i < this.times.length; i++) {
            if(this.times[i] > 0 && this.times2[i] > 0)
                // TODO: what to do if one is unreachable?
                this.delta[i] = this.times[i] - times2[i];
        }
    }

    protected void buildDeltaHistograms(PointSet targets) {
        int[] magSum = new int[times.length];

        for (Entry<String, int[]> cat : targets.properties.entrySet()) {
            // FIXME this appears to be summing the value of every column for each feature - what?
            String catId = cat.getKey();
            int[] values = cat.getValue();
            for(int i = 0; i < values.length; i++){
                magSum[i] += values[i];
            }	
        }

        this.histograms.put("scenario1", new Histogram(times, magSum));
        this.histograms.put("scenario2", new Histogram(times2, magSum));
    }
}