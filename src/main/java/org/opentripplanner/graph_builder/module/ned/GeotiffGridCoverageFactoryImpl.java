package org.opentripplanner.graph_builder.module.ned;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.Hints;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Implementation of ElevationGridCoverageFactory for Geotiff data.
 */
public class GeotiffGridCoverageFactoryImpl implements ElevationGridCoverageFactory {

    private static final Logger LOG = LoggerFactory.getLogger(GeotiffGridCoverageFactoryImpl.class);

    private final File path;
    private GridCoverage2D coverage;

    public GeotiffGridCoverageFactoryImpl(File path) {
        this.path = path;
    }

    @Override
    public GridCoverage2D getGridCoverage() {
        try {
            // There is a serious standardization failure around the axis order of WGS84. See issue #1930.
            // GeoTools assumes strict EPSG axis order of (latitude, longitude) unless told otherwise.
            // Both NED and SRTM data use the longitude-first axis order, so OTP makes grid coverages
            // for unprojected DEMs assuming coordinates are in (longitude, latitude) order.
            Hints forceLongLat = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
            GeoTiffFormat format = new GeoTiffFormat();
            GeoTiffReader reader = format.getReader(path, forceLongLat);
            coverage = reader.read(null);
            LOG.info("Elevation model CRS is: {}", coverage.getCoordinateReferenceSystem2D());
        } catch (IOException e) {
            throw new RuntimeException("Error getting coverage automatically. ", e);
        }
        return coverage;
    }

    @Override
    public void checkInputs() {
        if (!path.canRead()) {
            throw new RuntimeException("Can't read elevation path: " + path);
        }
    }

    @Override
    public void setGraph(Graph graph) {
        //nothing to do here
    }

}