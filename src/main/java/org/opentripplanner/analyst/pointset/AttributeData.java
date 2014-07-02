package org.opentripplanner.analyst.pointset;

import java.io.Serializable;

/**
 * Holds the attributes for a single feature when it's being loaded from
 * GeoJSON. Not used for the OTP internal representation, just during the
 * loading step. TODO Should potentially be used in CSV loading for
 * uniformity of methods
 */
public class AttributeData implements Serializable {
	private static final long serialVersionUID = 8485179983326803500L;
	
	public String category;
	public String attribute;
	public int value;
	
	public AttributeData() {
		this(null,null,0);
	}

	public AttributeData(String category, String attribute, int value) {
		this.category = category;
		this.attribute = attribute;
		this.value = value;
	}
	
	public String toString(){
		return category+"."+attribute+":"+value;
	}
}