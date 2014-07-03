package org.opentripplanner.analyst;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.collect.Maps;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Just like IndicatorLite, plus this carries around the TimeSurface
 */

public class Indicator extends IndicatorLite {

	protected PointSet origins; // the origins, which correspond one-to-one with the Quantiles array
	protected PointSet targets; // the targets that were checked for reachability in this indicator
    protected TimeSurface surface; // actually there is one per origin, not a single one!
    
    Map<String,Integer> idTimeIndex = new ConcurrentHashMap<String,Integer>();
     
    public Indicator (SampleSet samples, TimeSurface surface, boolean retainTimes) {
        super(samples, surface); // for now we only do one-to-many
        this.surface = surface;
        this.targets = samples.pset;
    }
    
    public Integer getTime(String id) {
		return null;
    	
//    	if(times != null){
//    		synchronized(idTimeIndex) {
//        		if(idTimeIndex.keySet().size() < this.times[0].length) {
//            		idTimeIndex.clear();
//            		int i = 0;
//            		for(String sampleId : this.targets.ids) {
//            			idTimeIndex.put(sampleId, this.times[0][i]);
//            			i++;
//            		}
//            	}
//        	}
//        	return idTimeIndex.get(id);
//    	}
//    	else
//    		return null;
    	
    }
    
    protected void writeStructured(int i , JsonGenerator jgen) throws IOException{
//    	super.writeStructured(i, jgen);
//		/*
//		 * Write out travel times to each target ID if this is a
//		 * detailed indicator.
//		 */
//		if (this instanceof Indicator && times != null) {
//			((Indicator) this).targets.writeTimes(jgen, times[i]);
//		}
    }
		
}
