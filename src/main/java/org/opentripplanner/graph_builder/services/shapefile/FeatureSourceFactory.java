package org.opentripplanner.graph_builder.services.shapefile;

import org.geotools.data.FeatureSource;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;

public interface FeatureSourceFactory {
    FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource();

    void cleanup();
    
    /** @see GraphBuilderModule#checkInputs()  */
    void checkInputs();
}
