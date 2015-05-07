/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module.shapefile;

import java.util.HashMap;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/**
 * Handles marking certain types of streets/bike lanes as more or less safe for bike trips.
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

        int directionFeature = 3; // Default to applying the safety feature in both directions
                                  // (useful if the dataset doesn't include direction information)
        if (directionAttributeName != null) {
        	String directionKey = feature.getAttribute(directionAttributeName).toString();
        	if (directionKey != null) {
        		directionFeature = directions.get(directionKey.toString());
        	}
        }

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

    /**
     * @param safetyAttributeName
     *            The name of the attribute used when calculating the feature's safety.
     */
    public void setSafetyAttributeName(String safetyAttributeName) {
        this.safetyAttributeName = safetyAttributeName;
    }

    /**
     * @param directionAttributeName
     *            The name of the attribute used when calculating the direction of the
     *            street/bikelane for which that the safety feature should apply.
     */
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
