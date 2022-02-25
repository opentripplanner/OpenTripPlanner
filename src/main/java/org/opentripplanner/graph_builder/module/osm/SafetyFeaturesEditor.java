package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.common.model.P2;

import java.beans.PropertyEditorSupport;

public class SafetyFeaturesEditor extends PropertyEditorSupport {
    private P2<Double> value;

    public void setAsText(String safetyFeatures) {
        String[] strings = safetyFeatures.split(",");
        value = new P2<Double>(Double.parseDouble(strings[0]), Double.parseDouble(strings[1]));
    }

    public String getAsText() {
        return value.first + ", " + value.second;
    }

    public Object getValue() {
        return value;
    }

    @SuppressWarnings("unchecked")
    public void setValue(Object object) {
        value = (P2<Double>) object;
    }
}
