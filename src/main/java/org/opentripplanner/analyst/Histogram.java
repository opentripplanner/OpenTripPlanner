package org.opentripplanner.analyst;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;

import java.io.IOException;
import java.io.Serializable;

/**
 * A pair of parallel histograms representing how many features are located at each amount of travel time 
 * away from from a single origin. One array contains the raw counts of features (e.g. number of places of employment
 * M minutes from home) and the other array contains the weighted sums of those features accounting for their 
 * magnitudes (e.g. the number of jobs in all places of employment M minutes away from home).
 * All time values are rounded down into 1-minute bins (0-60 seconds = minute 0, 61-120 = min 1, etc.)
 */
public class Histogram implements Serializable {

    /**
     * The number features that can be reached within each one-minute bin. Index 0 is 0-1 minutes, index 50 is 50-51 
	 * minutes, etc. The features are not weighted by their magnitudes, so values represent (for example) the number of 
	 * places of employment that can be reached rather than the total number of jobs in all those places of employment.
     */
    public final int[] counts;
    
    /**
     * The weighted sum of all features that can be reached within each one-minute bin.
	 * Index 0 is 0-1 minutes, index 50 is 50-51 minutes, etc.
	 * Features are weighted by their magnitudes, so values represent (for example) the total number of jobs in
	 * all accessible places of employment, rather than the number of places of employment.
     */
    public final int[] sums;

    /**
	 * Given parallel arrays of travel times and magnitudes for any number of destination features, construct 
	 * histograms that represent the distribution of individual features and total opportunities as a function of
	 * travel time. The length of the arrays containing these histograms will be equal to the maximum travel time
	 * specified in the original search request, in minutes.
     * @param times the time at which each destination is reached. The array will be destructively sorted in place.
     * @param weights the weight or magnitude of each destination reached. it is parallel to times.
     */
    public Histogram (int[] times, int[] weights) {

		// FIXME Hard coded array sizes there is a PR for this
		int tmpCounts[] = new int[1000];
    	int tmpSums[] = new int[1000];
    	
    	int uppperBound = 0;
    	
    	for(int i = 0; i < times.length; i++) {
    		
    		if(times[i] < 0 || times[i] == Integer.MAX_VALUE)
    			continue;
    	
    		int minuteBin = (int) Math.floor(times[i] / 60.0);
    		
    		tmpCounts[minuteBin] += 1; 
    		tmpSums[minuteBin] += weights[i];
    				
    		if(minuteBin > uppperBound)
    			uppperBound = minuteBin;
    	}
    	
    	counts = new int[uppperBound];
    	sums = new int[uppperBound];

    	for(int i = 0; i < uppperBound; i++) {
    		counts[i] = tmpCounts[i];
    		sums[i] = tmpSums[i];
    	}   	
    }
    
	/**
	 * Serialize this pair of histograms out as a JSON document using the given JsonGenerator. The format is:
	 * <pre> {
	 *   sums: [],
	 *   counts: []     
	 * } </pre> 
	 */
    public void writeJson(JsonGenerator jgen) throws JsonGenerationException, IOException {
		// The number of features reached during each minute, ignoring their magnitudes
    	jgen.writeArrayFieldStart("sums"); {
    		for(int sum : sums) {
    			jgen.writeNumber(sum);
    		}
    	}
    	jgen.writeEndArray();
		// The total number of opportunities reached during each minute (the sum of the features' magnitudes)
    	jgen.writeArrayFieldStart("counts"); {
    		for(int count : counts) {
    			jgen.writeNumber(count);
    		}
    	}
    	jgen.writeEndArray();
    }
}
