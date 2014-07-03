package org.opentripplanner.analyst.pointset;

public class CategoryMetadata {

	public String label;
	public Style style;
	public String id;
	
	public CategoryMetadata(String id){
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
