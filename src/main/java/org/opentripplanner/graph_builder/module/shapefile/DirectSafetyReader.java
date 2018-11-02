package org.opentripplanner.graph_builder.module.shapefile;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

/*
 * Read safety factors directly from shapefiles (contributed by Guillaume Barreau)
 */
public class DirectSafetyReader implements SimpleFeatureConverter<P2<Double>> {
    private String safetyAttributeName;

    public static final P2<Double> oneone = new P2<Double>(1.0, 1.0);

    @Override
    public P2<Double> convert(SimpleFeature feature) {
        Double d = (Double) feature.getAttribute(safetyAttributeName);
        if (d == null) {
            return oneone;
        }
        return new P2<Double>(d, d);
    }

    public void setSafetyAttributeName(String safetyAttributeName) {
        this.safetyAttributeName = safetyAttributeName;
    }
}
