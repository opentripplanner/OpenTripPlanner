package org.opentripplanner.graph_builder.module.shapefile;

import org.opengis.feature.simple.SimpleFeature;
import org.opentripplanner.graph_builder.services.shapefile.SimpleFeatureConverter;

public class FeatureIdConverter implements SimpleFeatureConverter<String> {

    @Override
    public String convert(SimpleFeature feature) {
        return feature.getID();
    }

}
