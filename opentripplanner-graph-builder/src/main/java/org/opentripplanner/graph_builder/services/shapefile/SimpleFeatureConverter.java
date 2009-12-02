package org.opentripplanner.graph_builder.services.shapefile;

import org.opengis.feature.simple.SimpleFeature;

public interface SimpleFeatureConverter<T> {
    public T convert(SimpleFeature feature);
}
