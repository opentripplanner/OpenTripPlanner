package org.opentripplanner.geocoder.reverse;

import java.io.File;
import java.io.IOException;

import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;


public class ShapefileBoundaryResolver implements BoundaryResolver {

    private String shapefile, nameField;
    private FeatureCollection collection;
    
    public void setShapefile(String shapefile) {
        this.shapefile = shapefile;
    }
    
    public void setNameField(String nameField) {
        this.nameField = nameField;
    }
    
    public void initialize() throws IOException {
        if (shapefile == null) {
            throw new IllegalStateException("shapefile path is not set");
        }

        // get feature results
        ShapefileDataStore store = new ShapefileDataStore(new File(shapefile).toURI().toURL());
        String name = store.getTypeNames()[0];
        FeatureSource source = store.getFeatureSource(name);
        collection = source.getFeatures();
    }
    
    @Override
    public String resolve(double x, double y) {
        System.out.println("x="+x+", y="+y);
        FeatureIterator<Feature> iterator = collection.features();
        while( iterator.hasNext() ){
             SimpleFeature feature = (SimpleFeature) iterator.next();

             Geometry geom = (Geometry) feature.getDefaultGeometry();
             
             GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();             
             Coordinate coord = new Coordinate(x, y);
             Point point = geometryFactory.createPoint(coord);
             
             //System.out.println("checking "+point.toString());
             if(geom.contains(point)) {
                 return feature.getAttribute(this.nameField).toString();
             }
        }
        return null;
        		
        
    }

}
