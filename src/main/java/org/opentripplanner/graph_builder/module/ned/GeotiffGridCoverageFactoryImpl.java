package org.opentripplanner.graph_builder.module.ned;

import java.io.File;
import java.io.IOException;
import javax.media.jai.InterpolationBilinear;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.util.factory.Hints;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.graph_builder.services.ned.ElevationGridCoverageFactory;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of ElevationGridCoverageFactory for Geotiff data.
 */
public class GeotiffGridCoverageFactoryImpl implements ElevationGridCoverageFactory {

  private static final Logger LOG = LoggerFactory.getLogger(GeotiffGridCoverageFactoryImpl.class);

  private final DataSource input;
  private final double elevationUnitMultiplier;
  private GridCoverage2D coverage;

  public GeotiffGridCoverageFactoryImpl(DataSource dataSource, double elevationUnitMultiplier) {
    this.input = dataSource;
    this.elevationUnitMultiplier = elevationUnitMultiplier;
  }

  public GeotiffGridCoverageFactoryImpl(File path) {
    this(new FileDataSource(path, FileType.DEM), 1.0);
  }

  /**
   * Wraps the underlying grid coverage instance with an interpolator that can be used in a specific
   * thread.
   */
  @Override
  public GridCoverage getGridCoverage() {
    return NoDataGridCoverage.create(
      Interpolator2D.create(getUninterpolatedGridCoverage(), new InterpolationBilinear())
    );
  }

  @Override
  public double elevationUnitMultiplier() {
    return elevationUnitMultiplier;
  }

  @Override
  public void checkInputs() {
    if (!input.exists()) {
      throw new RuntimeException("Can't read elevation path: " + input.path());
    }
  }

  /**
   * Nothing to do here. File should already exist on computer.
   */
  @Override
  public void fetchData(Graph graph) {}

  /**
   * Lazy-creates a GridCoverage2D instance by loading the specific elevation file into memory.
   * During a refactor in the year 2020, the code at one point was written such that each coverage
   * instance was created and wrapped in the Interpolator2D interpolator for each thread to use.
   * However, benchmarking showed that this caused longer run times which is likely due to too much
   * memory competing for a slot in the processor cache.
   */
  public GridCoverage2D getUninterpolatedGridCoverage() {
    if (coverage == null) {
      try {
        // There is a serious standardization failure around the axis order of WGS84. See issue #1930.
        // GeoTools assumes strict EPSG axis order of (latitude, longitude) unless told otherwise.
        // Both NED and SRTM data use the longitude-first axis order, so OTP makes grid coverages
        // for unprojected DEMs assuming coordinates are in (longitude, latitude) order.
        Hints forceLongLat = new Hints(Hints.FORCE_LONGITUDE_FIRST_AXIS_ORDER, Boolean.TRUE);
        GeoTiffFormat format = new GeoTiffFormat();
        GeoTiffReader reader = format.getReader(getSource(), forceLongLat);
        coverage = reader.read(null);
        LOG.debug("Elevation model CRS is: {}", coverage.getCoordinateReferenceSystem2D());
      } catch (IOException e) {
        throw new RuntimeException("Error getting coverage automatically. ", e);
      }
    }
    return coverage;
  }

  private Object getSource() {
    return input.asInputStream();
  }
}
