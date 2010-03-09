package org.opentripplanner.graph_builder.impl.shapefile;

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

public class CaseBasedBooleanConverter implements SimpleFeatureConverter<Boolean> {
    public boolean defaultValue = false;

    private Map<String, Boolean> values = new HashMap<String, Boolean>();

    private String attributeName;

    public CaseBasedBooleanConverter() {

    }

    public CaseBasedBooleanConverter(String attributeName) {
        this.attributeName = attributeName;
    }

    public CaseBasedBooleanConverter(String attributeName,
            Boolean defaultValue) {
        this.attributeName = attributeName;
        this.defaultValue = defaultValue; 
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setDefaultValue(Boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setValues(Map<String, Boolean> values) {
        this.values = values; 
    }

    @Override
    public Boolean convert(SimpleFeature feature) {
        String key = feature.getAttribute(attributeName).toString();
        Boolean value = values.get(key);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }
}
