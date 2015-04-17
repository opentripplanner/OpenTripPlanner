package org.opentripplanner.analyst.pointset;

import java.io.Serializable;

/**
 * Contains display and styling information for a single property of features in a PointSet.
 */
public class PropertyMetadata implements Serializable{

	/** A PointSet-unique property identifier */
	public String id;

	/** A short description of this property for use in a legend or menu */
	public String label;

	/** The display style for features having this property (e.g. color) */
	public Style style;

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
