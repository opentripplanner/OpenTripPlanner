package org.opentripplanner.graph_builder.services.shapefile;

import org.opengis.feature.simple.SimpleFeature;

/**
 * Interface for converters from an opengis @{link org.opengis.feature.simple.SimpleFeature} 
 * to an object of type T
 * 
 * @param <T> the type to convert to.
 */
public interface SimpleFeatureConverter<T> {
    public T convert(SimpleFeature feature);
}
