package org.opentripplanner.graph_builder.services.shapefile;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public interface FeatureSourceFactory {
    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource();
}
