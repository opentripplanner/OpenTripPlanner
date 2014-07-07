package org.opentripplanner.analyst;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResultFeatureWithTimes extends ResultFeature {

	private static final long serialVersionUID = -6723127825189535112L;
    
	public String id;
	public Map<String,Histogram> histograms;
	
	public PointSet targets;
	public int[] times;
	
	public Map<String,Integer> timeIdMap = new ConcurrentHashMap<String,Integer>();
	
	public ResultFeatureWithTimes(){
		super();
	}
	
	public Integer getTime(String featureId) {
		return timeIdMap.get(featureId);
	}
	
    public static ResultFeatureWithTimes eval(SampleSet samples, TimeSurface surface) {
    	ResultFeatureWithTimes ret = new ResultFeatureWithTimes();
    	
    	// keep targets and times in resultfeature object
        ret.targets = samples.pset;
        // Evaluate the surface at all points in the pointset
        ret.times = samples.eval(surface);
        
        ret.buildHistograms(ret.times, ret.targets);
        
		int i = 0;
		for(String id : ret.targets.ids) {
			
			ret.timeIdMap.put(id, ret.times[i]);
			
			i++;
		}
        
        return ret;
    }

}