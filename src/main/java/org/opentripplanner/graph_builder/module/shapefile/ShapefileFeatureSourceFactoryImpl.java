package org.opentripplanner.graph_builder.module.shapefile;

import java.io.File;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opentripplanner.graph_builder.services.shapefile.FeatureSourceFactory;

public class ShapefileFeatureSourceFactoryImpl implements FeatureSourceFactory {

    private File _path;
    private ShapefileDataStore dataStore;

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
            dataStore = new ShapefileDataStore(_path.toURI().toURL());

            String typeNames[] = dataStore.getTypeNames();
            String typeName = typeNames[0];

            return dataStore.getFeatureSource(typeName);
        } catch (Exception ex) {
            throw new IllegalStateException("error creating feature source from shapefile: path="
                    + _path, ex);
        }
    }
    
    @Override
    public void cleanup() {
        dataStore.dispose();
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read Shapefile path: " + _path);
        }
    }
}
