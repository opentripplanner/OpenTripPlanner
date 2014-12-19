package org.opentripplanner.analyst;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResultSetWithTimes extends ResultSet {

	private static final long serialVersionUID = -6723127825189535112L;
    
	public int[] times;
	
	public Map<String,Integer> timeIdMap = new ConcurrentHashMap<String,Integer>();
	
	public ResultSetWithTimes() {
		
	}
	
	public ResultSetWithTimes(SampleSet samples, TimeSurface surface) {
		id = samples.pset.id + "_" + surface.id + "_times";
		
		// Evaluate the surface at all points in the pointset
        times = samples.eval(surface);
        
        buildHistograms(times, samples.pset);
        
		int i = 0;
		for(String id : samples.pset.ids) {
			timeIdMap.put(id, times[i]);
			i++;
		}
	}
	
	public Integer getTime(String featureId) {
		return timeIdMap.get(featureId);
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