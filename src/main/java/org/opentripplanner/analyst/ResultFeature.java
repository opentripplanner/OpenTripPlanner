package org.opentripplanner.analyst;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ResultFeature {

	private static final long serialVersionUID = -6723127825189535112L;
    
	/*
	 * The time to reach each target, for each origin.
	 */
	public Map<String,Histogram> histograms;
	
    public ResultFeature (SampleSet samples, TimeSurface surface) {
    	histograms = new HashMap<String,Histogram>();
    	
        PointSet targets = samples.pset;
        // Evaluate the surface at all points in the pointset
        int[] times = samples.eval(surface);
        for (Entry<String, int[]> cat : targets.categories.entrySet()) {
        	String catId = cat.getKey();
        	int[] mags = cat.getValue();
        	
        	this.histograms.put(catId, new Histogram(times, mags));
        }
    }
    
    /**
     * Each origin will yield CSV with columns category,min,q25,q50,q75,max
     * Another column for the origin ID would allow this to extend to many-to-many.
     */
    void toCsv() {
    	
    	

    }
    
	public void writeJson(OutputStream output) {
		// TODO Auto-generated method stub
		
	}
    


}