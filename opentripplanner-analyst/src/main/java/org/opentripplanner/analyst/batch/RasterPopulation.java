package org.opentripplanner.analyst.batch;

import java.io.File;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Individuals should be in row-major order
 */
public class RasterPopulation extends BasicPopulation {

    private static final long serialVersionUID = 20120201L; //YYYYMMDD
    private static final Logger LOG = LoggerFactory.getLogger(RasterPopulation.class);
    
    @Setter int width, height; 
    @Setter double originLon, originLat; // southwest corner
    @Setter double lonStep, latStep;
    
    @PostConstruct
    public void loadIndividuals() {
        LOG.debug("Loading population from raster file {}", sourceFilename);
        try {
            File rasterFile = new File(sourceFilename);

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
                    this.add(individual);
                }
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Error loading population from raster file: ", ex);
        }
        LOG.debug("Done loading raster from file.");
    }
}
