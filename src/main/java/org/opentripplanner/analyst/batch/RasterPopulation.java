package org.opentripplanner.analyst.batch;

import org.geotools.coverage.grid.*;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.geometry.DirectPosition;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Individuals should be in a random-access-friendly List implementation in row-major order
 */
public class RasterPopulation extends BasicPopulation {

    private static final Logger LOG = LoggerFactory.getLogger(RasterPopulation.class);

    /* configuration fields */
    public int rows = 200, cols = 200; // these are the raster (gridEnvelope) dimensions
    public double left, right, top, bottom; // bounding box values in CRS
    public int band = 0; // raster band to read
    public double unitySeconds = 0; // scale output values so unity=1. 0 to turn off. 
    
    /* derived fields */
    protected CoordinateReferenceSystem coverageCRS; // from input raster or config string
    protected GridEnvelope2D gridEnvelope; // the envelope for the pixels
    protected ReferencedEnvelope refEnvelope; // the envelope in the CRS
    protected GridGeometry2D gridGeometry; // relationship between the grid envelope and the CRS envelope
    protected MathTransform gridToWGS84; // composed transform from the grid to the CRS to WGS84
    protected GridCoverage2D coverage; // null if synthetic (no coverage to load)
    
    @Override
    public void writeAppropriateFormat(String outFileName, ResultSet results) {
        this.writeGeotiff(outFileName, results);
    }

    public void setUnityMinutes(double minutes) {
        this.unitySeconds = minutes * 60;
    }
    
    public void writeGeotiff(String fileName, ResultSet results) {
        LOG.info("writing geotiff.");
        float[][] imagePixelData = new float[rows][cols]; 
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = row * cols + col;
                float pixel = (float) (results.results[index]);
                if (unitySeconds > 0)
                    pixel /= unitySeconds;
                imagePixelData[row][col] = pixel;
            }
        }
        GridCoverage2D coverage = new GridCoverageFactory().create("OTPAnalyst", imagePixelData, refEnvelope);
        try {
            GeoTiffWriteParams wp = new GeoTiffWriteParams();
            wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
            wp.setCompressionType("LZW");
            ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
            params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
            GeoTiffWriter writer = new GeoTiffWriter(new File(fileName));
            writer.write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
        } catch (Exception e) {
            LOG.error("exception while writing geotiff.", e);
        }
        LOG.info("done writing geotiff.");
    }
    
    @Override
    public void createIndividuals() {
        LOG.info("Loading population from raster file {}", sourceFilename);
        try {
            File rasterFile = new File(sourceFilename);
            // determine file format and CRS, then load raster
            AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
            AbstractGridCoverage2DReader reader = format.getReader(rasterFile);
            GridCoverage2D coverage = reader.read(null);
            this.coverageCRS = coverage.getCoordinateReferenceSystem();
            GridGeometry2D gridGeometry = coverage.getGridGeometry();
            GridEnvelope2D gridEnvelope = gridGeometry.getGridRange2D();
            gridGeometry.getGridToCRS();
            // because we may want to produce an empty raster rather than loading one, alongside the coverage we 
            // store the row/col dimensions and the referenced envelope in the original coordinate reference system.
            this.cols  = gridEnvelope.width;
            this.rows = gridEnvelope.height;
            this.createIndividuals0();
        } catch (Exception ex) {
            throw new IllegalStateException("Error loading population from raster file: ", ex);
        }
        LOG.info("Done loading raster from file.");
    }
    
    /** Shared internal createIndividuals method allowing synthetic subclass to reuse projection code */
    protected void createIndividuals0() {
        MathTransform tr; 
        try {
            final CoordinateReferenceSystem WGS84 = CRS.decode("EPSG:4326", true);
            tr = CRS.findMathTransform(coverageCRS, WGS84);
        } catch (Exception e) {
            LOG.error("error creating CRS transform.", e);
            return;
        }
        // grid coordinate object to be reused for reading each cell in the raster
        GridCoordinates2D coord = new GridCoordinates2D();
        // evaluating a raster returns an array of results, in this case 1D
        int[] val = new int[1];
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                coord.x = col;
                coord.y = row;
                try {
                    // find coordinates for current raster cell in raster CRS
                    DirectPosition sourcePos = gridGeometry.gridToWorld(coord);
                    // TODO: we are performing 2 transforms here, it would probably be more efficient to compose 
                    // the grid-to-crs and crs-to-WGS84 transforms into grid-to-WGS84.
                    // cf. MathTransformFactory and CoordinateOperationFactory
                    // convert coordinates in raster CRS to WGS84
                    DirectPosition targetPos = tr.transform(sourcePos, null);
                    double lon = targetPos.getOrdinate(0);
                    double lat = targetPos.getOrdinate(1);
                    // evaluate using grid coordinates, which should be more efficient than using world coordinates
                    if (coverage != null)
                        coverage.evaluate(coord, val); 
                    // add this grid cell to the population
                    String label = row + "_" + col;
                    Individual individual = new Individual(label, lon, lat, val[band]);
                    this.addIndividual(individual);
                } catch (Exception e) {
                    LOG.error("error creating individuals for raster", e);
                    return;
                }
            }
        }
    }
    
}
