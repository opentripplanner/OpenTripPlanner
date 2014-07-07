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
	
	public ResultFeature(){
		histograms = new HashMap<String,Histogram>();
	}
	
    public static ResultFeature eval(SampleSet samples, TimeSurface surface) {
    	ResultFeature ret = new ResultFeature();
    	
        PointSet targets = samples.pset;
        // Evaluate the surface at all points in the pointset
        int[] times = samples.eval(surface);
        for (Entry<String, int[]> cat : targets.properties.entrySet()) {
        	String catId = cat.getKey();
        	int[] mags = cat.getValue();
        	
        	ret.histograms.put(catId, new Histogram(times, mags));
        }
        
        return ret;
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