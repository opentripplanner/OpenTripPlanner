package org.opentripplanner.graph_builder.services.ned;

import org.geotools.api.coverage.Coverage;
import org.geotools.coverage.grid.GridCoverage2D;
import org.opentripplanner.routing.graph.Graph;

/**
 * Factory interface specifying the ability to generate GeoTools {@link GridCoverage2D} objects
 * representing National Elevation Dataset (NED) raster data.
 *
 * @author demory
 */

public interface ElevationGridCoverageFactory {
  /** Creates a new coverage instance from files already fetched */
  Coverage getGridCoverage();

  /**
   * Unit conversion multiplier for elevation values. No conversion needed if the elevation values
   * are defined in meters in the source data. If, for example, decimetres are used in the source
   * data, this should be set to 0.1 in build-config.json.
   */
  double elevationUnitMultiplier();

  void checkInputs();

  /**
   * Sets the graph of the factory and initiates the fetching of data that is not present in the
   * cache
   */
  void fetchData(Graph graph);
}
