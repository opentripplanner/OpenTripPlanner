package org.opentripplanner.analyst.batch.factory;

import java.io.File;
import java.io.PrintWriter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.Query;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.Hints;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opentripplanner.analyst.batch.Individual;
import org.opentripplanner.analyst.batch.Population;
import org.opentripplanner.analyst.batch.RasterPopulation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

@Component
public class PopulationFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PopulationFactory.class);

    @Autowired
    private IndividualFactory individualFactory;
    
    public Population fromCSV(String filename) {
        Population population= new Population();
//        for (String line : file) {
//            Individual i = individualFactory.build(line.split(','));
//            population.individuals.add(i);
//        }
        return population;
    }

    public Population fromShapefile(
            String filename, 
            String idAttribute, 
            String dataAttribute) {
        
        Population population = new Population();
        LOG.debug("Loading population from shapefile {}", filename);
        LOG.debug("Feature attributes: id in {}, data in {}", idAttribute, dataAttribute);
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
            int i=0, nonNull=0;
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
                if (idAttribute == null) {
                    id = Integer.toString(i);
                } else {
                    id = feature.getAttribute(idAttribute).toString();
                }
                double data = (Double) feature.getAttribute(dataAttribute);
                
                Individual individual = individualFactory.build(
                        id, point.getX(), point.getY(), data);
                population.add(individual);
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
        return population;
    }
    
    /** Load a raster file as an otp batch analysis population */
    public RasterPopulation fromRaster(String filename) {
        RasterPopulation population = null;
        LOG.debug("Loading population from raster file {}", filename);
        try {
            File rasterFile = new File(filename);

            // determine file format and CRS, then load raster
            AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
            AbstractGridCoverage2DReader reader = format.getReader(rasterFile);
            CoordinateReferenceSystem sourceCRS = reader.getCrs();
            GridCoverage2D cov = reader.read(null);

            CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326", true);
            MathTransform tr = CRS.findMathTransform(sourceCRS, WGS84);

            // add param for crop envelope? autocrop to graph area?
            /*
            // envelope around our area of interest (petite couronne)
            ReferencedEnvelope wgsEnv = new ReferencedEnvelope(1.86, 2.76, 48.52, 49.1, WGS84);
            // reproject the envelope to the raster's CRS, longitude first
            ReferencedEnvelope sourceEnv = wgsEnv.transform(sourceCRS, true);
            // crop raster to reprojected envelope
            cov = (GridCoverage2D) Operations.DEFAULT.crop(cov, sourceEnv);
            // fetch grid information from the new cropped raster
             */

            GridGeometry2D gridGeometry = cov.getGridGeometry();
            GridEnvelope2D gridEnvelope = gridGeometry.getGridRange2D();
            
            int width  = gridEnvelope.width;
            int height = gridEnvelope.height;
            population = new RasterPopulation(width, height);

            // grid coordinate object to be reused for reading each cell in the raster
            GridCoordinates2D coord = new GridCoordinates2D();
            // evaluating a raster returns an array of results, in this case 1D
            int[] val = new int[1];
            for (int gy = gridEnvelope.y, iy = 0; iy < height; gy++, iy++) {
                for (int gx = gridEnvelope.x, ix = 0; ix < width; gx++, ix++) {
                    coord.x = gx;
                    coord.y = gy;
                    // find coordinates for current raster cell in raster CRS
                    DirectPosition sourcePos = gridGeometry.gridToWorld(coord);
                    cov.evaluate(sourcePos, val);
                    // convert coordinates in raster CRS to WGS84
                    DirectPosition targetPos = tr.transform(sourcePos, null);
                    // add this grid cell to the population
                    Individual individual = individualFactory.build(
                            ix + "," + iy,
                            targetPos.getOrdinate(0),
                            targetPos.getOrdinate(1), 
                            val[0]);
                    population.add(individual);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error loading population from raster file: ", ex);
        }
        LOG.debug("Done loading raster from file.");
        return population;
    }


}
