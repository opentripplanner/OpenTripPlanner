package org.opentripplanner.graph_builder.impl;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.LineString;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.opengis.coverage.Coverage;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * This graph builder module imports a CSV file containing scattered points, each of which
 * associates a congestion factor with lat,lon coordinates, interpolates those values
 * and applies them to road speeds.
 */
public class CongestionGraphBuilder implements GraphBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(CongestionGraphBuilder.class);
    private static final double MIN_SPEED = 00.1; // m/sec
    private static final double MAX_SPEED = 30.0; // m/sec

    private final InverseDistanceInterpolator interpolator;

    /* True means input values are speeds in m/sec rather than scaling factors. */
    private boolean absolute = false;

    public CongestionGraphBuilder (String filename) {
        interpolator = InverseDistanceInterpolator.fromCSV(filename);
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        LOG.info("Scaling street speeds to account for congestion.");
        for (PlainStreetEdge pse : Iterables.filter(graph.getEdges(), PlainStreetEdge.class)) {
            CoordinateSequence coords = pse.getGeometry().getCoordinateSequence();
            if (coords == null || coords.size() == 0) continue;
            double avg = 0;
            for (int c = 0; c < coords.size(); c++) {
                avg += interpolator.interpolate(coords.getX(c), coords.getY(c));
            }
            avg /= coords.size();
            double speed = absolute ? avg : pse.getCarSpeed() * avg;
            // Clamp speed range and update edge
            if (speed < MIN_SPEED) speed = MIN_SPEED;
            if (speed > MAX_SPEED) speed = MAX_SPEED;
            // LOG.info("edge {} speed {} scaled by {} to {}", pse.getName(), pse.getCarSpeed(), avg, speed);
            pse.setCarSpeed((float)speed);
        }
        LOG.info("Done scaling street speeds to account for congestion.");
    }

    @Override
    public List<String> provides() {
        return Arrays.asList("streets");
    }

    @Override
    public List<String> getPrerequisites() {
        return Arrays.asList("streets");
    }

    @Override
    public void checkInputs() {

    }

// COVERAGE VERSION:
//
//    public CongestionGraphBuilder (String sourceFilename) {
//        try {
//            File rasterFile = new File(sourceFilename);
//            // Determine file format and CRS, then load raster
//            AbstractGridFormat format = GridFormatFinder.findFormat(rasterFile);
//            AbstractGridCoverage2DReader reader = format.getReader(rasterFile);
//            coverage = reader.read(null);
//        } catch (Exception ex) {
//            throw new IllegalStateException("Error loading raster file: ", ex);
//        }
//        LOG.info("Done loading raster.");
//    }

//    @Override
//    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
//        CoordinateReferenceSystem crs = DefaultEngineeringCRS.CARTESIAN_2D;
//        double[] value = new double[1]; // reusable array to hold interpolation results
//        for (PlainStreetEdge pse : Iterables.filter(graph.getEdges(), PlainStreetEdge.class)) {
//            CoordinateSequence coords = pse.getGeometry().getCoordinateSequence();
//            if (coords == null || coords.size() == 0) continue;
//            double avg = 0;
//            for (int c = 0; c < coords.size(); c++) {
//                // Make GeoTools/OpenGIS DirectPosition from the JTS Coordinate
//                coverage.evaluate(new DirectPosition2D(crs, coords.getX(c), coords.getY(c)), value);
//                avg += value[0];
//            }
//            avg /= coords.size();
//            double speed = absolute ? avg : pse.getCarSpeed() * avg;
//            // Clamp speed range and update edge
//            if (speed < MIN_SPEED) speed = MIN_SPEED;
//            if (speed > MAX_SPEED) speed = MAX_SPEED;
//            pse.setCarSpeed((float)speed);
//        }
//    }

}