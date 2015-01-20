package org.opentripplanner.analyst;

import java.util.Map.Entry;

public class ResultSetDelta extends ResultSetWithTimes {

    private static final long serialVersionUID = -6723127825189535112L;

    public int[] times2;
    public int[] delta;

    public ResultSetDelta(SampleSet samples1, SampleSet samples2, TimeSurface surface1, TimeSurface surface2) {
        id = samples1.pset.id + "_" + surface1.id + "_" + surface2.id + "_delta";

        // Evaluate the surface at all points in the pointset
        this.times = samples1.eval(surface1);
        this.times2 = samples2.eval(surface2);

        this.delta = new int[times2.length];

        for(int i = 0; i < this.times.length; i++) {
            if(this.times[i] > 0 && this.times2[i] > 0)
                this.delta[i] = this.times[i] - times2[i];
        }

        buildDeltaHistograms(samples1.pset);
    }

    protected void buildDeltaHistograms(PointSet targets) {
        int[] magSum = new int[times.length];

        for (Entry<String, int[]> cat : targets.properties.entrySet()) {
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