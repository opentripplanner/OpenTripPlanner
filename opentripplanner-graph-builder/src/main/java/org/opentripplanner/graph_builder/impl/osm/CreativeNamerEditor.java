package org.opentripplanner.graph_builder.impl.osm;

import java.beans.PropertyEditorSupport;

public class CreativeNamerEditor extends PropertyEditorSupport {
	private CreativeNamer value;

	public void setAsText(String pattern) {
		value = new CreativeNamer(pattern);
	}

	public String getAsText() {
		return value.getCreativeNamePattern();
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object object) {
		value = (CreativeNamer) object;
	}
}
