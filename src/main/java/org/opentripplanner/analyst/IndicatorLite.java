package org.opentripplanner.analyst;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.opentripplanner.analyst.PointSet.Attribute;
import org.opentripplanner.analyst.PointSet.Category;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * A TimeSurface is evaluated at all the points in a PointSet to yield an Indicator.
 *
 * These are represented as a constrained format of GeoJSON.
 * They provide cumulative distributions for access to the opportunities in the PointSet
 * with respect to travel time from a particular origin.
 *
 * Indicators these are one of the three main web analyst resources:
 * Pointsets
 * Indicators
 * TimeSurfaces
 *
 * note: A TimeSurface is a lot like a Coverage. A Pointset is a lot like a FeatureCollection.
 *
 * An Indicator is structured exactly like a pointset.
 * In fact, it could be a subclass of Pointset with an extra field in each Attribute.
 *
 * Is it a one-to-many indicator, or many to many? Attributes.quantiles is an array, so
 * it's many-to-many.
 */

public class IndicatorLite extends PointSet {

	private static final long serialVersionUID = -6723127825189535112L;

	protected PointSet targets; // the targets that were checked for reachability in this indicator
    
	/*
	 * The time to reach each target, for each origin.
	 */
	public int[][] times;
	
    Map<String,Integer> idTimeIndex = new ConcurrentHashMap<String,Integer>();

    public IndicatorLite (SampleSet samples, TimeSurface surface, boolean retainTimes) {
        super(1); // for now we only do one-to-many
        this.targets = samples.pset;
        // Perform a deep copy of everything but the actual magnitudes for the attributes
        for (Category cat : targets.categories.values()) {
            this.categories.put(cat.id, new Category(cat));
        }
        // Evaluate the surface at all points in the pointset
        int[] times = samples.eval(surface);
        for (Category cat : categories.values()) {
            for (Attribute attr : cat.attributes.values()) {
                attr.quantiles = new Quantiles[1];
                attr.quantiles[0] = new Quantiles(times, attr.magnitudes, 10);
            }
        }
        /* If requested, provide a detailed map from target IDs to travel times. */
        if (retainTimes) {
            this.times = new int[1][]; // we only support one-to-many currently.
            this.times[0] = times;
        }
    }
    
    public Integer getTime(String id) {
    	
    	if(times != null){
    		synchronized(idTimeIndex) {
        		if(idTimeIndex.keySet().size() < this.times[0].length) {
            		idTimeIndex.clear();
            		int i = 0;
            		for(String sampleId : this.targets.ids) {
            			idTimeIndex.put(sampleId, this.times[0][i]);
            			i++;
            		}
            	}
        	}
        	return idTimeIndex.get(id);
    	}
    	else
    		return null;
    	
    }
    
    /**
     * Each origin will yield CSV with columns category,min,q25,q50,q75,max
     * Another column for the origin ID would allow this to extend to many-to-many.
     */
    void toCsv() {
    	
    	

    }
    
    protected void writeStructured(int i , JsonGenerator jgen) throws IOException{
    	super.writeStructured(i, jgen);
		/*
		 * Write out travel times to each target ID if this is a
		 * detailed indicator.
		 */
		if (this instanceof Indicator && times != null) {
			((Indicator) this).targets.writeTimes(jgen, times[i]);
		}
    }

}