package org.opentripplanner.analyst.batch;

import java.io.File;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class ShapefilePopulation extends BasicPopulation {

    private static final Logger LOG = LoggerFactory.getLogger(ShapefilePopulation.class);

    @Setter String labelAttribute;

    @Setter String inputAttribute;

    public void loadIndividuals() {
        String filename = this.sourceFilename;
        LOG.debug("Loading population from shapefile {}", filename);
        LOG.debug("Feature attributes: input data in {}, labeled with {}", inputAttribute, labelAttribute);
        try {
            File file = new File(filename);
            FileDataStore store = FileDataStoreFinder.getDataStore(file);
            SimpleFeatureSource featureSource = store.getFeatureSource();

            CoordinateReferenceSystem sourceCRS = featureSource.getInfo().getCRS();
            CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326", true);

            Query query = new Query();
            query.setCoordinateSystem(sourceCRS);
            query.setCoordinateSystemReproject(WGS84);
            SimpleFeatureCollection featureCollection = featureSource.getFeatures(query);

            SimpleFeatureIterator it = featureCollection.features();
            int i = 0, nonNull = 0;
            while (it.hasNext()) {
                SimpleFeature feature = it.next();
                Geometry geom = (Geometry) feature.getDefaultGeometry();
                Point point = null;
                if (geom instanceof Point) {
                    point = (Point) geom;
                } else if (geom instanceof Polygon) {
                    point = ((Polygon) geom).getCentroid();
                } else if (geom instanceof MultiPolygon) {
                    point = ((MultiPolygon) geom).getCentroid();
                } else {
                    throw new IllegalStateException("Shapefile must contain either points or polygons.");
                }
                String id;
                if (labelAttribute == null) {
                    id = Integer.toString(i);
                } else {
                    id = feature.getAttribute(labelAttribute).toString();
                }
                double data = (Double) feature.getAttribute(inputAttribute);

                Individual individual = individualFactory.build(id, point.getX(), point.getY(), data);
                this.addIndividual(individual);
                i += 1;
                if (individual.sample != null)
                    nonNull += 1;
            }
            LOG.debug("found vertices for {} features out of {}", nonNull, i);
            it.close();
        } catch (Exception ex) {
            throw new IllegalStateException("Error loading population from shapefile ", ex);
        }
        LOG.debug("Done loading shapefile.");
    }

}
