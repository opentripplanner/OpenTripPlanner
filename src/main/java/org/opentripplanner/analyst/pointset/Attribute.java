package org.opentripplanner.analyst.pointset;

import java.io.Serializable;

import org.opentripplanner.analyst.Histogram;

/**
 * The leaves of the OTPA structured properties, with one magnitude or
 * cumulative curve per feature.
 */
public class Attribute extends Structured implements Serializable{

	private static final long serialVersionUID = -6525345212775303158L;
	
	public int[] magnitudes;
	public Histogram[] histogram; // An array, one per origin. Length is 1 until we
							// support many-to-many.
	
	public Attribute(){
		//blank constructor for deserialization
		super();
	}

	/** Shallow copy constructor. */
	public Attribute(String id) {
		super(id);
	}

	public Attribute(Attribute other) {
		super(other);
		this.magnitudes = other.magnitudes;
		this.histogram = other.histogram;
	}
	
	public Attribute slice(int start, int end) {
		Attribute ret = new Attribute(this.id);
		ret.description = this.description;
		ret.label = this.label;
		ret.style = this.style;
		
		ret.magnitudes = new int[end-start];
		
		if(this.histogram!=null){
			ret.histogram = new Histogram[end-start];
		}
		
		int n=0;
		for(int i=start; i<end; i++){
			ret.magnitudes[n] = this.magnitudes[i];
			
			if(this.histogram!=null){
				ret.histogram[n] = this.histogram[i];
			}
		}
		
		return ret;
	}
	
	public int[] getMagnitudes(){
		return this.magnitudes;
	}
	
	public Histogram[] getHistogram(){
		return this.histogram;
	}
}