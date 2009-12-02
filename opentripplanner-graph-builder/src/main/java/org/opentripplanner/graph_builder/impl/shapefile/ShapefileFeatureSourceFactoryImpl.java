package org.opentripplanner.graph_builder.impl.shapefile;

import java.io.File;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;

public class ShapefileFeatureSourceFactoryImpl implements FeatureSourceFactory {

    private File _path;

    public ShapefileFeatureSourceFactoryImpl() {
        
    }
    
    public ShapefileFeatureSourceFactoryImpl(File path) {
        _path = path;
    }

    public void setPath(File path) {
        _path = path;
    }

    @Override
    public FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource() {

        try {
            ShapefileDataStore dataStore = new ShapefileDataStore(_path.toURI().toURL());

            String typeNames[] = dataStore.getTypeNames();
            String typeName = typeNames[0];

            return dataStore.getFeatureSource(typeName);
        } catch (Exception ex) {
            throw new IllegalStateException("error creating feature source from shapefile: path="
                    + _path, ex);
        }
    }
}
