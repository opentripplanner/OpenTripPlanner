package org.opentripplanner.graph_builder.impl.shapefile;

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/**
 * This converter handles bike lanes/paths/routes.
 */
public class CaseBasedBicycleSafetyFeatureConverter implements SimpleFeatureConverter<P2<Double>> {

    private String safetyAttributeName;
    private String directionAttributeName;

    private Map<String, Double> safetyFeatures = new HashMap<String, Double>();
    private Map<String, Integer> directions = new HashMap<String, Integer>();
    public static final P2<Double> oneone = new P2<Double>(1.0, 1.0);

    @Override
    public P2<Double> convert(SimpleFeature feature) {
        String safetyKey = feature.getAttribute(safetyAttributeName).toString();
        Double safetyFeature = safetyFeatures.get(safetyKey);
        if (safetyFeature == null)
            return oneone;

        String directionKey = feature.getAttribute(directionAttributeName).toString();
        int directionFeature = directions.get(directionKey);

        return new P2<Double>((directionFeature & 0x1) == 0 ? 1.0 : safetyFeature,
                (directionFeature & 0x2) == 0 ? 1.0 : safetyFeature);
    }

    public CaseBasedBicycleSafetyFeatureConverter(String safetyAttributeName,
            String directionAttributeName) {
        this.safetyAttributeName = safetyAttributeName;
        this.directionAttributeName = directionAttributeName;
    }

    public CaseBasedBicycleSafetyFeatureConverter() {
    }

    public void setSafetyAttributeName(String safetyAttributeName) {
        this.safetyAttributeName = safetyAttributeName;
    }

    public void setDirectionAttributeName(String directionAttributeName) {
        this.directionAttributeName = directionAttributeName;
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

    /**
     * Maps the direction value to a number representing the direction that the bike safety feature
     * goes. The number is 1 for a safety feature that goes with the road geometry, 2 for a safety
     * feature that goes against it, and 3 for a safety feature that goes both ways.
     * @param directionValues
     */
    public void setDirection(Map<String, String> directionValues) {
        for (Map.Entry<String, String> entry : directionValues.entrySet()) {
            String attributeValue = entry.getKey();
            String featureName = entry.getValue();

            Integer direction = Integer.valueOf(featureName);
            addDirection(attributeValue, direction);
        }
    }

    public void addDirection(String attributeValue, Integer direction) {
        directions.put(attributeValue, direction);
    }
}
