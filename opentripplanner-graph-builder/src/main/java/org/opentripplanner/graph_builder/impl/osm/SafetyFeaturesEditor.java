package org.opentripplanner.graph_builder.impl.osm;

import java.beans.PropertyEditorSupport;

import org.opentripplanner.common.model.P2;

public class SafetyFeaturesEditor extends PropertyEditorSupport {
	private P2<Double> value;
	
	public void setAsText(String safetyFeatures) {
		String[] strings = safetyFeatures.split(",");
		value = new P2<Double>(Double.parseDouble(strings[0]),
				Double.parseDouble(strings[1]));
	}
	
	public String getAsText() {
		return value.getFirst() + ", " + value.getSecond();
	}
	
	public Object getValue() {
		return value;
	}
	
	@SuppressWarnings("unchecked")
	public void setValue(Object object) {
		value = (P2<Double>) object;
	}
}
