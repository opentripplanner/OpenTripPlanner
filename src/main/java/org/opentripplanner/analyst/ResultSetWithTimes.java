package org.opentripplanner.analyst;

public class ResultSetWithTimes extends ResultSet {

    private static final long serialVersionUID = -6723127825189535112L;

    public int[] times;
    
    public ResultSetWithTimes() {

    }

    public ResultSetWithTimes(SampleSet samples, TimeSurface surface) {
        super(samples, surface);
        
        // Evaluate the surface at all points in the pointset
        times = samples.eval(surface);
    }

    public Integer minTime() {

        int minTime = Integer.MAX_VALUE;

        for(int t : times) {
            if(t < minTime) 
                minTime = t;
        }

        if(minTime == Integer.MAX_VALUE)
            minTime = 0;

        return minTime;
    }

    public Integer maxTime() {

        int maxTime = 0;
        for(int t : times) {
            if(t > maxTime) 
                maxTime = t;
        }

        return maxTime;

    }
}