package org.opentripplanner.graph_builder.impl.shapefile;

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

public class CaseBasedBicycleSafetyFeatureConverter implements
        SimpleFeatureConverter<Double> {

    private String attributeName;

    private Map<String, Double> safetyFeatures = new HashMap<String, Double>();

    @Override
    public Double convert(SimpleFeature feature) {
        String key = feature.getAttribute(attributeName).toString();
        Double safetyfeature = safetyFeatures.get(key);
        if (safetyfeature == null)
            safetyfeature = 1.0;
        return safetyfeature;
    }

    public CaseBasedBicycleSafetyFeatureConverter(String attributeName) {
        this.attributeName = attributeName;
    }

    public CaseBasedBicycleSafetyFeatureConverter() {
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setSafety(Map<String, String> safetyValues) {
        for (Map.Entry<String, String> entry : safetyValues.entrySet()) {
            String attributeValue = entry.getKey();
            String featureName = entry.getValue();

            Double safety = Double.valueOf(featureName);
            addSafety(attributeValue, safety);
        }
    }

    public void addSafety(String attributeValue, Double safety) {
        safetyFeatures.put(attributeValue, safety);
    }
}
