package org.opentripplanner.analyst.pointset;

import java.io.Serializable;

/**
 * A base class for the various levels of the Analyst GeoJSON "structured"
 * attributes.
 */
abstract class Structured implements Serializable{
	private static final long serialVersionUID = 1001662681953599754L;
	
	public String id;
	public String label;
	public String description;
	public Style style;
	
	public Structured(){
		this.id=null;
	}

	public Structured(String id) {
		this.id = id;
	}

	public Structured(Structured other) {
		this.id = other.id;
		this.label = other.label;
		this.style = other.style;
		this.description = other.description;
	}

	public void addStyle(String attribute, String value) {
		if (style == null) {
			style = new Style();
		}
		style.attributes.put(attribute, value);
	}
	
	public String getId(){
		return this.id;
	}
	
	public String getlabel(){
		return this.label;
	}
	
	public String getDescription(){
		return this.description;
	}
}