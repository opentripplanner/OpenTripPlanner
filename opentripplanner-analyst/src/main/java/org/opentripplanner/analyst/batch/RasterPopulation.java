package org.opentripplanner.analyst.batch;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferShort;
import java.awt.image.DataBufferUShort;
import java.awt.image.RenderedImage;
import java.io.File;

import javax.annotation.PostConstruct;

import lombok.Setter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opentripplanner.analyst.core.Sample;
import org.opentripplanner.analyst.parameter.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Individuals should be in row-major order
 */
public class RasterPopulation extends BasicPopulation {

    private static final long serialVersionUID = 20120201L; //YYYYMMDD
    private static final Logger LOG = LoggerFactory.getLogger(RasterPopulation.class);

    @Setter int width, height; // cells 
    @Setter double left, right, top, bottom;
    @Setter double lonStep, latStep;
    
    protected GridCoverage2D cov;
    protected CoordinateReferenceSystem covCRS; 
    
    protected void createIndividuals() {
        this.covCRS = cov.getCoordinateReferenceSystem();
        MathTransform tr; 
        try {
            final CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326", true);
            tr = CRS.findMathTransform(covCRS, WGS84);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

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
        
        this.width  = gridEnvelope.width;
        this.height = gridEnvelope.height;

        // grid coordinate object to be reused for reading each cell in the raster
        GridCoordinates2D coord = new GridCoordinates2D();
        // evaluating a raster returns an array of results, in this case 1D
        int[] val = new int[1];
        for (int gy = gridEnvelope.y, iy = 0; iy < height; gy++, iy++) {
            for (int gx = gridEnvelope.x, ix = 0; ix < width; gx++, ix++) {
                coord.x = gx;
                coord.y = gy;
                try {
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
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
    
    @Override
    public void writeOriginalFormat(String fileName) {
        writeGeotiff(fileName);
    }

    public void writeGeotiff(String fileName) {
        LOG.info("writing geotiff.");
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        short[] imagePixelData = ((DataBufferUShort)image.getRaster().getDataBuffer()).getData();
        int i = 0;
        for (Individual indiv : this.getIndividuals()) {
            short pixel = (short) indiv.output;
            if (pixel < 0)
                pixel = 0;
            imagePixelData[i] = pixel;
            i++;
        }
        // replace coverage... we should maybe store only the envelope, or the gridgeom in the syntheticrasterpop
        this.cov = new GridCoverageFactory().create(cov.getName(), image, cov.getEnvelope());
        try {
            GeoTiffWriteParams wp = new GeoTiffWriteParams();
            wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
            wp.setCompressionType("LZW");
            ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
            params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
            new GeoTiffWriter(new File(fileName)).write(this.cov, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
        } catch (Exception e) {
            LOG.error("exception while writing geotiff.");
            e.printStackTrace();
        }
        LOG.info("done writing geotiff.");
    }
    
    @PostConstruct
    public void loadIndividuals() {
        LOG.info("Loading population from raster file {}", sourceFilename);
        try {
            File rasterFile = new File(sourceFilename);
            // determine file format and CRS, then load raster
            AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
            AbstractGridCoverage2DReader reader = format.getReader(rasterFile);
            this.cov = reader.read(null);
        } catch (Exception ex) {
            throw new IllegalStateException("Error loading population from raster file: ", ex);
        }
        LOG.info("Done loading raster from file.");
    }
    
}
