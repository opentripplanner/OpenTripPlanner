package org.opentripplanner.analyst.pointset;

public class PropertyMetadata {

	public String label;
	public Style style;
	public String id;
	
	public PropertyMetadata(String id){
		this.style = new Style();
		this.id = id;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void addStyle(String styleAttribute, String styleValue) {
		this.style.attributes.put(styleAttribute, styleValue);
	}

}
