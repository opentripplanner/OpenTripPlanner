package org.opentripplanner.graph_builder.impl;

import com.beust.jcommander.internal.Lists;
import com.csvreader.CsvReader;
import com.vividsolutions.jts.geom.Envelope;
import lombok.AllArgsConstructor;
import org.apache.commons.math3.util.FastMath;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Inverse distance weighting 2D interpolation.
 * See http://en.wikipedia.org/wiki/Inverse_distance_weighting
 * Time complexity of each interpolation operation is linear in the number of interpolation points.
 * Do not use this with huge point sets.
 */
public class InverseDistanceInterpolator {

    private static final Logger LOG = LoggerFactory.getLogger(InverseDistanceInterpolator.class);
    private static final double RADIUS_M = 300; // smoothing radius
    private static final double RADIUS_DEG = RADIUS_M / 111111;
    private static final double RADIUS_DEG_2 = RADIUS_DEG * RADIUS_DEG;

    private List<Point> points = Lists.newArrayList();

    public void addPoint(double x, double y, double z) {
        points.add(new Point(x, y, z));
    }

    /** There must be at least one point in the interpolator. */
    // Math.pow and sqrt are very slow.
    public double interpolate (double x, double y) {
        Point p0 = new Point(x, y, 0);
        double total = 0;
        double totalWeight = 0;
        for (Point p1 : points) {
            double d2 = p0.squaredDistance(p1); // twice as slow using non-squared distance
            // Avoid division by zero and smooth extreme points
            // by shifting past the part of the function near 0
            d2 += RADIUS_DEG_2;
            // if (d2 < RADIUS_DEG_2) d2 = RADIUS_DEG_2; // causes ringing
            // if (d2 == 0) return p1.z;
            d2 *= d2; // d^4 (regions are more distinct than d^2)
            //d2 *= d2; // d^8
            //d2 *= d2; // d^16
            double weight = 1.0 / d2;
            // Gaussian is slow and detail seems less apparent
            // double weight = Math.exp(-(d2 / (2.0 * VARIANCE)));
            total += p1.z * weight;
            totalWeight += weight;
        }
        // LOG.info("{}/{}", total, totalWeight);
        return total / totalWeight;
    }

    /**
     * Create an InverseDistanceInterpolator and load its values from a CSV file.
     */
    public static InverseDistanceInterpolator fromCSV (String filename) {
        LOG.info("Loading interpolator from CSV file {}", filename);
        InverseDistanceInterpolator idi = new InverseDistanceInterpolator();
        try {
            CsvReader reader = new CsvReader(filename, ',', Charset.forName("UTF8"));
            reader.readHeaders();
            while (reader.readRecord()) {
                try {
                    double y = Double.parseDouble(reader.get("lat"));
                    double x = Double.parseDouble(reader.get("lon"));
                    double z = Double.parseDouble(reader.get("val"));
                    idi.addPoint(x, y, z);
                } catch (NumberFormatException nfe) {
                    LOG.warn("Could not read record at line {}", reader.getCurrentRecord() + 2);
                }
            }
        } catch (Exception ex) {
            LOG.error("Exception loading CSV into interpolator: {}", ex.toString());
            ex.printStackTrace();
        }
        if (idi.points.isEmpty()) {
            LOG.error("Interpolator contains no points and is therefore invalid.");
            return null;
        }
        return idi;
    }

    @AllArgsConstructor
    private static class Point {
        private static double xscale = 0.6;
        double x, y, z;
        public double squaredDistance(Point other) {
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            dx *= xscale;
            return dx*dx + dy*dy;
        }
        public double distance(Point other) {
            return FastMath.sqrt(squaredDistance(other));
        }
    }

    /**
     * Call the interpolator at a regular grid of points and save the output for visualization.
     */
    public void writeGeotiff(String fileName, int cols, int rows) {
        Envelope env = new Envelope();
        for (Point point : points) env.expandToInclude(point.x, point.y);
        ReferencedEnvelope refEnv = new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84);
        float[][] imagePixelData = new float[rows][cols];
        GridCoverage2D coverage = new GridCoverageFactory().create("interpolated", imagePixelData, refEnv);
        MathTransform transform = coverage.getGridGeometry().getGridToCRS();
        LOG.info("Writing GeoTIFF.");
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                try {
                    DirectPosition2D wgsPosition = new DirectPosition2D();
                    transform.transform(new DirectPosition2D(col, row), wgsPosition);
                    float pixel = (float) interpolate(wgsPosition.x, wgsPosition.y);
                    imagePixelData[row][col] = pixel;
                } catch (TransformException e) {
                    e.printStackTrace();
                }
            }
        }
        // updating the backing pixel array above does not affect the existing coverage. make a new one.
        // TODO: make this less hackish
        coverage = new GridCoverageFactory().create("interpolated", imagePixelData, refEnv);
        try {
            GeoTiffWriteParams wp = new GeoTiffWriteParams();
            wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
            wp.setCompressionType("LZW");
            ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
            params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
            GeoTiffWriter writer = new GeoTiffWriter(new File(fileName));
            writer.write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
        } catch (Exception e) {
            LOG.error("exception while writing GeoTIFF.");
            e.printStackTrace();
        }
        LOG.info("Done writing GeoTIFF.");
    }

    public static void main(String[] params) {
        if (params.length != 1) {
            LOG.error("Supply the path to the input CSV file as a command line parameter.");
            System.exit(1);
        }
        InverseDistanceInterpolator idi = InverseDistanceInterpolator.fromCSV(params[0]);
        idi.writeGeotiff(params[0] + ".tiff", 1000, 800);
    }
}
